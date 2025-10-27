class CsvUploadResponseDTO {
  final String message;
  final int totalRecords;
  final int normalizedRecords;
  final int recordsWithErros;
  final String batchId;
  final String processingStatus;

  CsvUploadResponseDTO({required this.message, required this.totalRecords, required this.normalizedRecords,
    required this.recordsWithErros, required this.batchId, required this.processingStatus});
}