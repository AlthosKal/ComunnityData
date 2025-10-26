package com.senasoft.comunidataapi.csv.repository;

import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import com.senasoft.comunidataapi.csv.enums.ProblemCategory;
import com.senasoft.comunidataapi.csv.enums.ProcessingStatus;
import com.senasoft.comunidataapi.csv.enums.UrgencyLevel;
import com.senasoft.comunidataapi.csv.enums.Zone;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository para operaciones CRUD sobre reportes ciudadanos.
 */
@Repository
public interface CitizenReportRepository extends MongoRepository<CitizenReport, String> {

    // Consultas para Function Callbacks

    /** Filtra reportes por rango de edad */
    List<CitizenReport> findByEdadBetween(Integer minEdad, Integer maxEdad);

    /** Filtra reportes por ciudad */
    List<CitizenReport> findByCiudad(String ciudad);

    /** Filtra reportes por múltiples ciudades */
    List<CitizenReport> findByCiudadIn(List<String> ciudades);

    /** Filtra reportes por categoría del problema */
    List<CitizenReport> findByCategoriaProblema(ProblemCategory categoria);

    /** Filtra reportes por múltiples categorías */
    List<CitizenReport> findByCategoriaProblemaIn(List<ProblemCategory> categorias);

    /** Filtra reportes por nivel de urgencia */
    List<CitizenReport> findByNivelUrgencia(UrgencyLevel urgencia);

    /** Filtra reportes por múltiples niveles de urgencia */
    List<CitizenReport> findByNivelUrgenciaIn(List<UrgencyLevel> urgencias);

    /** Filtra reportes por atención previa del gobierno */
    List<CitizenReport> findByAtencionPreviaGobierno(Boolean atencionPrevia);

    /** Filtra reportes por rango de fechas */
    List<CitizenReport> findByFechaReporteBetween(LocalDate fechaInicio, LocalDate fechaFin);

    /** Filtra reportes por zona */
    List<CitizenReport> findByZona(Zone zona);

    /** Filtra reportes con sesgo detectado */
    List<CitizenReport> findBySesgoDetectado(Boolean sesgoDetectado);

    /** Consultas combinadas */
    List<CitizenReport> findByCategoriaProblemaAndZona(
            ProblemCategory categoria, Zone zona);

    List<CitizenReport> findByCiudadAndCategoriaProblema(
            String ciudad, ProblemCategory categoria);

    List<CitizenReport> findByZonaAndNivelUrgencia(Zone zona, UrgencyLevel urgencia);

    /** Consultas para procesamiento */
    List<CitizenReport> findByEstadoProcesamiento(ProcessingStatus status);

    List<CitizenReport> findByBatchId(String batchId);

    Long countByEstadoProcesamiento(ProcessingStatus status);

    Long countByBatchId(String batchId);

    /** Consultas para estadísticas */
    @Query("{ 'estadoProcesamiento': 'COMPLETADO' }")
    List<CitizenReport> findAllCompletedReports();

    @Query("{ 'estadoProcesamiento': 'COMPLETADO', 'sesgoDetectado': false }")
    List<CitizenReport> findAllValidReports();

    /** Consulta para obtener reportes que necesitan embedding */
    @Query("{ 'estadoProcesamiento': 'PROCESANDO_IA', 'embedding': null }")
    List<CitizenReport> findReportsNeedingEmbedding();
}
