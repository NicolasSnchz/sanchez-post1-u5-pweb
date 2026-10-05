package com.ejemplo.servlet;

import com.ejemplo.model.Tarea;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// loadOnStartup = 1: init() corre al desplegar la app y no en la primera peticion
// a /tareas. Asi la lista ya esta en applicationScope aunque el usuario entre
// directo a /tareas/detalle despues de reiniciar Tomcat.
@WebServlet(name = "TareasServlet", urlPatterns = {"/tareas"}, loadOnStartup = 1)
public class TareasServlet extends HttpServlet {

    // La lista es variable de instancia porque es el estado compartido de toda
    // la aplicacion, no el dato de una peticion. Como la unica instancia del
    // Servlet la usan varios hilos a la vez, se usan estructuras seguras para
    // concurrencia en vez de ArrayList e int.
    private final List<Tarea> tareas = new CopyOnWriteArrayList<>();
    private final AtomicInteger contadorId = new AtomicInteger(1);

    @Override
    public void init() throws ServletException {
        Date hoy = new Date();
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Leer documentaci\u00f3n de Servlets",
            "Estudio", "Alta", sumarDias(hoy, 2)));
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Implementar ciclo GET/POST",
            "Estudio", "Alta", sumarDias(hoy, 3)));
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Preparar sustentaci\u00f3n del laboratorio",
            "Evaluaci\u00f3n", "Media", sumarDias(hoy, 7)));
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Revisar JSTL y Expression Language",
            "Estudio", "Baja", sumarDias(hoy, 10)));

        // Se expone la misma lista en el ServletContext (applicationScope)
        // para que DetalleTareaServlet la lea sin duplicar el estado
        getServletContext().setAttribute("tareas", tareas);
    }

    private Date sumarDias(Date base, int dias) {
        long unDiaMs = 24L * 60 * 60 * 1000;
        return new Date(base.getTime() + dias * unDiaMs);
    }

    /** GET /tareas: listar con filtro combinado (texto + categoria + prioridad) */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        boolean hayFiltroEnUrl = req.getParameter("q") != null
            || req.getParameter("cat") != null
            || req.getParameter("prioridad") != null;

        String filtroTexto;
        String filtroCategoria;
        String filtroPrioridad;

        if (hayFiltroEnUrl) {
            // El usuario aplico un filtro nuevo: se usa y se recuerda en sesion.
            // Va en sesion y no en el request porque debe sobrevivir a varias
            // peticiones (ir al detalle y volver no debe borrar el filtro).
            filtroTexto     = req.getParameter("q");
            filtroCategoria = req.getParameter("cat");
            filtroPrioridad = req.getParameter("prioridad");
            session.setAttribute("filtroTexto", filtroTexto);
            session.setAttribute("filtroCategoria", filtroCategoria);
            session.setAttribute("filtroPrioridad", filtroPrioridad);
        } else {
            // Sin parametros en la URL (ej. clic directo en "Tareas"):
            // se restaura el ultimo filtro guardado en la sesion, si existe
            filtroTexto     = (String) session.getAttribute("filtroTexto");
            filtroCategoria = (String) session.getAttribute("filtroCategoria");
            filtroPrioridad = (String) session.getAttribute("filtroPrioridad");
        }

        // Cada filtro vacio o ausente deja pasar todas las tareas, por eso
        // funcionan juntos en cualquier combinacion
        List<Tarea> resultado = tareas.stream()
            .filter(t -> filtroTexto == null || filtroTexto.isBlank()
                         || t.getTitulo().toLowerCase().contains(filtroTexto.toLowerCase()))
            .filter(t -> filtroCategoria == null || filtroCategoria.isBlank()
                         || t.getCategoria().equals(filtroCategoria))
            .filter(t -> filtroPrioridad == null || filtroPrioridad.isBlank()
                         || t.getPrioridad().equals(filtroPrioridad))
            .collect(Collectors.toList());

        List<String> categorias = tareas.stream()
            .map(Tarea::getCategoria).distinct().sorted()
            .collect(Collectors.toList());

        // La lista filtrada es solo de request: es el resultado de esta respuesta
        req.setAttribute("tareas", resultado);
        req.setAttribute("categorias", categorias);
        req.setAttribute("filtroTexto", filtroTexto);
        req.setAttribute("filtroCategoria", filtroCategoria);
        req.setAttribute("filtroPrioridad", filtroPrioridad);
        req.getRequestDispatcher("/WEB-INF/views/tareas.jsp")
           .forward(req, resp);
    }

    /** POST /tareas: agregar, eliminar, completar o identificar usuario */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");

        if ("agregar".equals(accion)) {
            String titulo = req.getParameter("titulo");
            if (titulo == null || titulo.isBlank()) {
                // Se reutiliza doGet para que la vista reciba tambien las
                // categorias y el filtro activo, no solo la lista sin filtrar
                req.setAttribute("error", "El t\u00edtulo no puede estar vac\u00edo");
                doGet(req, resp);
                return;
            }
            // Valores por defecto: el formulario rapido de la Parte 1 solo pide titulo
            tareas.add(new Tarea(contadorId.getAndIncrement(), titulo.trim(),
                "General", "Media", sumarDias(new Date(), 5)));

        } else if ("eliminar".equals(accion)) {
            int id = leerId(req);
            tareas.removeIf(t -> t.getId() == id);

        } else if ("completar".equals(accion)) {
            int id = leerId(req);
            tareas.stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .ifPresent(t -> t.setCompletada(true));

        } else if ("identificar".equals(accion)) {
            String nombre = req.getParameter("nombre");
            if (nombre != null && !nombre.isBlank()) {
                req.getSession().setAttribute("usuario", nombre.trim());
            }
        }

        // Patron PRG: redirigir despues de POST (todas las acciones)
        resp.sendRedirect(req.getContextPath() + "/tareas");
    }

    /** Lee el parametro id; si no es un numero devuelve -1 para no responder con un error 500 */
    private int leerId(HttpServletRequest req) {
        try {
            return Integer.parseInt(req.getParameter("id"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
