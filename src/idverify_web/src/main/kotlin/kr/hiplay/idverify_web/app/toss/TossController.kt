package kr.hiplay.idverify_web.app.toss

//import jakarta.servlet.http.HttpServletRequest
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.beans.factory.annotation.Qualifier
//import org.springframework.stereotype.Controller
//import org.springframework.ui.Model
//import org.springframework.ui.set
//import org.springframework.web.bind.annotation.GetMapping
//import org.springframework.web.bind.annotation.RequestMapping
//import org.springframework.web.servlet.LocaleResolver
//
//
//@Controller
//@RequestMapping("/toss")
//class TossController(var tossService: TossService) {
//    @Autowired
//    private lateinit var request: HttpServletRequest
//
//    @Autowired
//    @Qualifier("localeResolver")
//    private lateinit var localeResolver: LocaleResolver
//
//    @GetMapping("/start.html")
//    fun start(model: Model): String {
//        model["provider"] = "toss"
//        model["lang"] = localeResolver.resolveLocale(request)
//
//        return "identify/start"
//    }
//}
