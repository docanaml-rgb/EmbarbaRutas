import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
} from "firebase/firestore";

const PROJECT_ID = "embarba-rutas";
const FIRESTORE_HOST = "127.0.0.1";
const FIRESTORE_PORT = 8080;
const SUPERVISOR_A = "supervisor-a";
const SUPERVISOR_B = "supervisor-b";
const SUPERVISION_A = "SUP-TEST-A";
const SUPERVISION_B = "SUP-TEST-B";
const SUPERVISION_LEGACY = "SUP-TEST-LEGACY";

const currentDirectory = dirname(fileURLToPath(import.meta.url));
const rulesPath = resolve(currentDirectory, "..", "..", "firestore.rules");

let testEnvironment;

function supervisionData(supervisorId, estado = "Pendiente") {
  return {
    supervisorId,
    cliente: `Cliente ${supervisorId}`,
    direccion: "Av. Pruebas 123",
    distrito: "Lima",
    hora: "09:00",
    estado,
    latitud: -12.0464,
    longitud: -77.0428,
  };
}

function checklistData(overrides = {}) {
  return {
    versionChecklist: 2,
    limpiezaConservacionCabina: "Cumple",
    iluminacionCabina: "Cumple",
    botonesPanelControl: "No cumple",
    aperturaCierrePuertas: "Cumple",
    deteccionObstaculosPuertas: "Cumple",
    nivelacionPisos: "Cumple",
    ruidosVibraciones: "Cumple",
    indicadoresPisoDireccion: "Cumple",
    botonesLlamadaPisos: "Cumple",
    alarmaComunicacionEmergencia: "Cumple",
    senalizacionSeguridad: "Cumple",
    accesosLibresObstaculos: "Cumple",
    observacionesIncidencias: {
      botonesPanelControl: "El botón del tercer piso no responde.",
    },
    observaciones: "Registro de prueba",
    fechaRegistro: serverTimestamp(),
    ...overrides,
  };
}

function movilidadData(overrides = {}) {
  return {
    fechaRegistro: serverTimestamp(),
    motivo: "Supervisión de ascensor",
    medioTransporte: "Transporte público",
    gastoTotal: 12.5,
    ...overrides,
  };
}

function supervisorDb(uid) {
  return testEnvironment
    .authenticatedContext(uid, { email: `${uid}@embarba.test` })
    .firestore();
}

async function seedBaseData() {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await Promise.all([
      setDoc(
        doc(db, "supervisiones", SUPERVISION_A),
        supervisionData(SUPERVISOR_A),
      ),
      setDoc(
        doc(db, "supervisiones", SUPERVISION_B),
        supervisionData(SUPERVISOR_B),
      ),
      setDoc(
        doc(db, "supervisiones", SUPERVISION_LEGACY),
        {
          ...supervisionData(SUPERVISOR_A, "Finalizada"),
          horaFinalizacion: serverTimestamp(),
        },
      ),
    ]);
  });
}

async function seedSupervisionState(supervisionId, estado) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await updateDoc(
      doc(context.firestore(), "supervisiones", supervisionId),
      { estado },
    );
  });
}

async function seedChecklist(supervisionId) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(
      doc(
        context.firestore(),
        "supervisiones",
        supervisionId,
        "checklist",
        "resultado",
      ),
      checklistData(),
    );
  });
}

async function seedMovilidad(supervisionId, data = movilidadData()) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(
      doc(
        context.firestore(),
        "supervisiones",
        supervisionId,
        "movilidad",
        "registro",
      ),
      data,
    );
  });
}

async function finalizarComoAplicacion(db, supervisionId) {
  const supervisionRef = doc(db, "supervisiones", supervisionId);
  const checklistRef = doc(
    db,
    "supervisiones",
    supervisionId,
    "checklist",
    "resultado",
  );
  const movilidadRef = doc(
    db,
    "supervisiones",
    supervisionId,
    "movilidad",
    "registro",
  );

  return runTransaction(db, async (transaction) => {
    await transaction.get(supervisionRef);
    await transaction.get(checklistRef);
    await transaction.get(movilidadRef);
    transaction.update(supervisionRef, {
      estado: "Finalizada",
      horaFinalizacion: serverTimestamp(),
    });
  });
}

describe("Reglas de seguridad de Embarba Rutas", function () {
  this.timeout(15_000);

  before(async () => {
    testEnvironment = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        host: FIRESTORE_HOST,
        port: FIRESTORE_PORT,
        rules: readFileSync(rulesPath, "utf8"),
      },
    });
  });

  beforeEach(async () => {
    await testEnvironment.clearFirestore();
    await seedBaseData();
  });

  after(async () => {
    await testEnvironment?.cleanup();
  });

  it("permite consultar las supervisiones propias", async () => {
    const db = supervisorDb(SUPERVISOR_A);
    const ownQuery = query(
      collection(db, "supervisiones"),
      where("supervisorId", "==", SUPERVISOR_A),
    );

    const snapshot = await assertSucceeds(getDocs(ownQuery));
    const ids = snapshot.docs.map((item) => item.id);
    if (!ids.includes(SUPERVISION_A) || !ids.includes(SUPERVISION_LEGACY)) {
      throw new Error("La consulta no devolvió todas las supervisiones propias.");
    }
  });

  it("rechaza consultar las supervisiones de otro supervisor", async () => {
    const db = supervisorDb(SUPERVISOR_A);
    const foreignQuery = query(
      collection(db, "supervisiones"),
      where("supervisorId", "==", SUPERVISOR_B),
    );

    await assertFails(getDocs(foreignQuery));
  });

  it("rechaza leer directamente una supervisión ajena", async () => {
    const db = supervisorDb(SUPERVISOR_A);
    await assertFails(getDoc(doc(db, "supervisiones", SUPERVISION_B)));
  });

  it("permite registrar la llegada con los cuatro campos autorizados", async () => {
    const db = supervisorDb(SUPERVISOR_A);
    const supervisionRef = doc(db, "supervisiones", SUPERVISION_A);

    await assertSucceeds(
      updateDoc(supervisionRef, {
        estado: "En supervision",
        horaLlegada: serverTimestamp(),
        latitudLlegada: -12.0464,
        longitudLlegada: -77.0428,
      }),
    );
  });

  it("rechaza registrar llegada si también se modifica otro campo", async () => {
    const db = supervisorDb(SUPERVISOR_A);
    const supervisionRef = doc(db, "supervisiones", SUPERVISION_A);

    await assertFails(
      updateDoc(supervisionRef, {
        estado: "En supervision",
        horaLlegada: serverTimestamp(),
        latitudLlegada: -12.0464,
        longitudLlegada: -77.0428,
        cliente: "Cliente alterado",
      }),
    );
  });

  it("permite crear un checklist válido", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    const db = supervisorDb(SUPERVISOR_A);
    const checklistRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "checklist",
      "resultado",
    );

    await assertSucceeds(setDoc(checklistRef, checklistData()));
  });

  it("rechaza un checklist con una verificación faltante", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    const db = supervisorDb(SUPERVISOR_A);
    const checklistRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "checklist",
      "resultado",
    );
    const incompleto = checklistData();
    delete incompleto.accesosLibresObstaculos;

    await assertFails(setDoc(checklistRef, incompleto));
  });

  it("rechaza una incidencia sin observación", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    const db = supervisorDb(SUPERVISOR_A);
    const checklistRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "checklist",
      "resultado",
    );

    await assertFails(
      setDoc(checklistRef, checklistData({ observacionesIncidencias: {} })),
    );
  });

  it("rechaza una observación de incidencia para una respuesta Cumple", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    const db = supervisorDb(SUPERVISOR_A);
    const checklistRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "checklist",
      "resultado",
    );

    await assertFails(
      setDoc(
        checklistRef,
        checklistData({
          botonesPanelControl: "Cumple",
          observacionesIncidencias: {
            botonesPanelControl: "Esta observación ya no corresponde.",
          },
        }),
      ),
    );
  });

  it("rechaza sobrescribir un checklist existente", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const checklistRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "checklist",
      "resultado",
    );

    await assertFails(setDoc(checklistRef, checklistData()));
  });

  it("rechaza eliminar un checklist", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const checklistRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "checklist",
      "resultado",
    );

    await assertFails(deleteDoc(checklistRef));
  });

  for (const medioTransporte of [
    "Transporte público",
    "Taxi/aplicativo",
    "Movilidad propia",
    "Otro",
  ]) {
    it(`permite crear movilidad válida con ${medioTransporte}`, async () => {
      await seedSupervisionState(SUPERVISION_A, "En supervision");
      await seedChecklist(SUPERVISION_A);
      const db = supervisorDb(SUPERVISOR_A);
      const movilidadRef = doc(
        db,
        "supervisiones",
        SUPERVISION_A,
        "movilidad",
        "registro",
      );
      const especificacion = medioTransporte === "Otro"
        ? { especificacionMedioTransporte: "Bicicleta" }
        : {};

      await assertSucceeds(
        setDoc(movilidadRef, movilidadData({ medioTransporte, ...especificacion })),
      );
    });
  }

  it("rechaza crear movilidad antes del checklist", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    const db = supervisorDb(SUPERVISOR_A);
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "movilidad",
      "registro",
    );

    await assertFails(setDoc(movilidadRef, movilidadData()));
  });

  it("rechaza movilidad con un medio de transporte inválido", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "movilidad",
      "registro",
    );

    await assertFails(
      setDoc(movilidadRef, movilidadData({ medioTransporte: "Avión" })),
    );
  });

  it("rechaza Otro sin especificar el medio de transporte", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "movilidad",
      "registro",
    );

    await assertFails(
      setDoc(movilidadRef, movilidadData({ medioTransporte: "Otro" })),
    );
  });

  it("rechaza especificación de Otro con una opción distinta", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "movilidad",
      "registro",
    );

    await assertFails(
      setDoc(
        movilidadRef,
        movilidadData({ especificacionMedioTransporte: "Contenido anterior" }),
      ),
    );
  });

  it("rechaza la estructura anterior con horas de desplazamiento", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "movilidad",
      "registro",
    );

    await assertFails(
      setDoc(
        movilidadRef,
        movilidadData({ horaInicio: "08:15", horaFin: "10:30" }),
      ),
    );
  });

  it("rechaza movilidad de costo cero sin justificación", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "movilidad",
      "registro",
    );

    await assertFails(
      setDoc(movilidadRef, movilidadData({ gastoTotal: 0 })),
    );
  });

  it("permite movilidad de costo cero con justificación", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "movilidad",
      "registro",
    );

    await assertSucceeds(
      setDoc(
        movilidadRef,
        movilidadData({
          gastoTotal: 0,
          justificacionSinGasto: "El traslado se realizó caminando.",
        }),
      ),
    );
  });

  it("rechaza justificación sin gasto cuando el importe es mayor que cero", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "movilidad",
      "registro",
    );

    await assertFails(
      setDoc(
        movilidadRef,
        movilidadData({ justificacionSinGasto: "No debe guardarse." }),
      ),
    );
  });

  it("rechaza sobrescribir una movilidad existente", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    await seedMovilidad(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_A,
      "movilidad",
      "registro",
    );

    await assertFails(setDoc(movilidadRef, movilidadData()));
  });

  it("rechaza finalizar sin checklist aunque exista movilidad", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedMovilidad(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);

    await assertFails(finalizarComoAplicacion(db, SUPERVISION_A));
  });

  it("rechaza finalizar sin movilidad aunque exista checklist", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);

    await assertFails(finalizarComoAplicacion(db, SUPERVISION_A));
  });

  it("permite finalizar cuando checklist y movilidad son válidos", async () => {
    await seedSupervisionState(SUPERVISION_A, "En supervision");
    await seedChecklist(SUPERVISION_A);
    await seedMovilidad(SUPERVISION_A);
    const db = supervisorDb(SUPERVISOR_A);

    await assertSucceeds(finalizarComoAplicacion(db, SUPERVISION_A));
    const snapshot = await assertSucceeds(
      getDoc(doc(db, "supervisiones", SUPERVISION_A)),
    );
    if (snapshot.data()?.estado !== "Finalizada") {
      throw new Error("La supervisión no quedó finalizada.");
    }
  });

  it("permite consultar una supervisión antigua finalizada sin movilidad", async () => {
    const db = supervisorDb(SUPERVISOR_A);
    const historyQuery = query(
      collection(db, "supervisiones"),
      where("supervisorId", "==", SUPERVISOR_A),
      where("estado", "==", "Finalizada"),
    );
    const checklistRef = doc(
      db,
      "supervisiones",
      SUPERVISION_LEGACY,
      "checklist",
      "resultado",
    );
    const movilidadRef = doc(
      db,
      "supervisiones",
      SUPERVISION_LEGACY,
      "movilidad",
      "registro",
    );

    const history = await assertSucceeds(getDocs(historyQuery));
    const checklist = await assertSucceeds(getDoc(checklistRef));
    const movilidad = await assertSucceeds(getDoc(movilidadRef));

    if (!history.docs.some((item) => item.id === SUPERVISION_LEGACY)) {
      throw new Error("La supervisión antigua no aparece en el historial.");
    }
    if (checklist.exists() || movilidad.exists()) {
      throw new Error("La supervisión antigua debía carecer de subdocumentos.");
    }
  });
});
