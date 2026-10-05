<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Detalle de Tarea</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/estilos.css">
</head>
<body>
    <h1>Detalle de Tarea</h1>

    <p><a href="${pageContext.request.contextPath}/tareas">&larr; Volver al listado</a></p>

    <table class="detalle">
        <tr><th>Título</th><td><c:out value="${tarea.titulo}"/></td></tr>
        <tr><th>Categoría</th><td>${tarea.categoria}</td></tr>
        <tr><th>Prioridad</th><td>${tarea.prioridad}</td></tr>
        <tr><th>Fecha límite</th>
            <td><fmt:formatDate value="${tarea.fechaLimite}" pattern="dd/MM/yyyy"/></td></tr>
        <tr><th>Estado</th>
            <td>${tarea.completada ? "Completada" : "Pendiente"}</td></tr>
    </table>

    <c:if test="${not tarea.completada}">
        <form method="post" action="${pageContext.request.contextPath}/tareas">
            <input type="hidden" name="accion" value="completar">
            <input type="hidden" name="id" value="${tarea.id}">
            <button type="submit">Marcar como completada</button>
        </form>
    </c:if>
</body>
</html>
