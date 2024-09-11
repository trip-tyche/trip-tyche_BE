package com.fivefeeling.memory.global.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class OAuth2SuccessController {

  @GetMapping("/oauth2/success")
  public String oauth2Success(
      @RequestParam("userId") Long userId,
      @RequestParam("token") String token,
      Model model) {
    // 전달된 userId와 token 값을 모델에 추가하여 Thymeleaf 페이지에 전달
    model.addAttribute("userId", userId);
    model.addAttribute("token", token);
    return "oauth2Success"; // thymeleaf 템플릿 이름
  }
}
