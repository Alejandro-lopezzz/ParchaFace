    package com.alejo.parchaface.controller;

    import com.alejo.parchaface.model.Usuario;
    import com.alejo.parchaface.service.UsuarioService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @RestController
    @RequestMapping("/usuarios")
    public class UsuarioController {

        @Autowired
        private UsuarioService usuarioService;

        @GetMapping
        public List<Usuario> obtenerTodos() {
            return usuarioService.getAllUsuarios();
        }

        @GetMapping("/{id}")
        public Usuario obtenerPorId(@PathVariable Integer id) {
            return usuarioService.getUsuarioById(id);
        }

        @PostMapping
        public Usuario guardar(@RequestBody Usuario usuario) {
            return usuarioService.saveUsuario(usuario);
        }

        @PutMapping("/{id}")
        public Usuario actualizar(@PathVariable Integer id, @RequestBody Usuario usuario) {
            usuario.setId_usuario(id);
            return usuarioService.saveUsuario(usuario);
        }

        @DeleteMapping("/{id}")
        public void eliminar(@PathVariable Integer id) {
            usuarioService.deleteUsuario(id);
        }
    }
