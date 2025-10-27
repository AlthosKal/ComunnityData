// ignore: avoid_web_libraries_in_flutter
import 'dart:html' as html;
import 'dart:typed_data';

/// Implementación web para descarga de archivos usando dart:html
void downloadFile(Uint8List bytes, String fileName) {
  try {
    // Crear un blob con los datos
    final blob = html.Blob([bytes]);

    // Crear una URL para el blob
    final url = html.Url.createObjectUrlFromBlob(blob);

    // Crear un elemento anchor temporal
    final anchor = html.AnchorElement(href: url)
      ..setAttribute('download', fileName)
      ..style.display = 'none';

    // Agregar al DOM, hacer clic y remover
    html.document.body?.children.add(anchor);
    anchor.click();
    html.document.body?.children.remove(anchor);

    // Liberar la URL del blob
    html.Url.revokeObjectUrl(url);

    print('✓ Archivo descargado en web: $fileName');
  } catch (e) {
    print('✗ Error al descargar archivo en web: $e');
    throw Exception('Error al descargar archivo: $e');
  }
}
