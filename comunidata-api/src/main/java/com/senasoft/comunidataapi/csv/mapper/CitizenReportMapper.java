package com.senasoft.comunidataapi.csv.mapper;

import com.senasoft.comunidataapi.csv.dto.response.CitizenReportResponseDTO;
import com.senasoft.comunidataapi.csv.entity.CitizenReport;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** Mapper para convertir entre CitizenReport entity y DTOs. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CitizenReportMapper {

    CitizenReportResponseDTO toResponseDTO(CitizenReport entity);

    CitizenReport toEntity(CitizenReportResponseDTO dto);
}
