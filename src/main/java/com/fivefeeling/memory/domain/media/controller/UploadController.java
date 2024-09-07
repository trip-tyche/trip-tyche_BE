package com.fivefeeling.memory.domain.media.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class UploadController {

  @GetMapping("/upload/{tripId}")
  public String showUploadPage(@PathVariable Long tripId, Model model) {
    model.addAttribute("tripId", tripId);
    return "upload";
  }
}
