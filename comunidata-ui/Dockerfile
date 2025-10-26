FROM nginx:alpine

# Elimina las configuraciones por defecto
RUN rm /etc/nginx/conf.d/default.conf
RUN rm /etc/nginx/nginx.conf

# ✅ Instalar curl para health check
RUN apk add --no-cache curl

# Copia las configuraciones personalizadas
COPY nginx.conf /etc/nginx/nginx.conf
COPY default.conf /etc/nginx/conf.d/

# Copia el contenido estático de Flutter Web
COPY build/web /usr/share/nginx/html

# Ajustar permisos para el usuario nginx
RUN chown -R nginx:nginx /usr/share/nginx/html

# Exponer puerto 80 para HTTP
EXPOSE 80

# Health check para ECS
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]