class CitizenReportResponseDTO {
  final String id;
  final int age;
  final String city;
  final String comment;
  final ProblemCategory categoryProblem;
  final UrgencyLevel urgencyLevel;
  final LocalDate reportDate;
  final bool governmentPreAttention;
  final Zone zone;
  final bool biasDetected;
  final String descriptionBias;
  final ProcessingStatus processingStatus;
  final DateTime importDate;
  final DateTime processDate;
}