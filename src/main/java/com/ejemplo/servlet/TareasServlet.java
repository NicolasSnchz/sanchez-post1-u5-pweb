package com.ejemplo.servlet;

import com.ejemplo.model.Tarea;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet(name = "TareasServlet", urlPatterns = {"/tareas"})
public class TareasServlet extends HttpServlet {

    // La lista es variable de instancia porque es el estado compartido de toda
    // la aplicacion, no el dato de una peticion. Como la unica instancia del
    // Servlet la usan varios hilos a la vez, se usan estructuras seguras para
    // concurrencia en vez de ArrayList e int.
    private final List<Tarea> tareas = new CopyOnWriteArrayList<>();
    private final AtomicInteger contadorId = new AtomicInteger(1);

    @Override
    public void init() throws ServletException {
        // Cargar datos de ejemplo al iniciar
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Leer documentaci\u00f3n de Servlets"));
        tareas.add(new Tarea(contadorId.getAndIncrement(), "Implementar ciclo GET/POST"));
    }

    /** GET /tareas: mostrar lista */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("tareas", tareas);
        req.getRequestDispatcher("/WEB-INF/views/tareas.jsp")
           .forward(req, resp);
    }

    /** POST /tareas: agregar o eliminar tarea */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String accion = req.getParameter("accion");

        if ("agregar".equals(accion)) {
            // titulo es variable local: es un dato propio de esta peticion
            String titulo = req.getParameter("titulo");
            if (titulo == null || titulo.isBlank()) {
                req.setAttribute("error", "El t\u00edtulo no puede estar vac\u00edo");
                req.setAttribute("tareas", tareas);
                req.getRequestDispatcher("/WEB-INF/views/tareas.jsp")
                   .forward(req, resp);
                return;
            }
            tareas.add(new Tarea(contadorId.getAndIncrement(), titulo.trim()));

        } else if ("eliminar".equals(accion)) {
            int id = Integer.parseInt(req.getParameter("id"));
            tareas.removeIf(t -> t.getId() == id);
        }

        // Patron PRG: redirigir despues de POST para que recargar no reenvie el formulario
        resp.sendRedirect(req.getContextPath() + "/tareas");
    }
}
