package krilovs.andrejs.chess.application

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class PageController {

  @GetMapping("/")
  fun home(): String {
    return "redirect:/chess/"
  }

  @GetMapping("/chess", "/chess/")
  fun chess(): String {
    return "forward:/chess/index.html"
  }
}