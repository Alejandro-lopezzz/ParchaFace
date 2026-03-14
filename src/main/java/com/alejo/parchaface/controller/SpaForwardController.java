package com.alejo.parchaface.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

  @GetMapping({
    "/",
    "/community",
    "/explore",
    "/login",
    "/register",
    "/perfil",
    "/event-detail/**",
    "/community/**"
  })
  public String forwardSpaRoutes() {
    return "forward:/index.html";
  }
}
