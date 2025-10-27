import 'dart:typed_data';

/// Stub implementation para descarga de archivos
/// Este archivo se usa cuando no se puede determinar la plataforma
void downloadFile(Uint8List bytes, String fileName) {
  throw UnsupportedError('La descarga de archivos no está soportada en esta plataforma');
}
