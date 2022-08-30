package org.ergoplatform.mosaik.example

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest

@RestController
@CrossOrigin
class TokenBurnController() {
    @GetMapping("/")
    fun mainPage(): ModelAndView {
        // we always serve nobrowser error page for the main url. If the request came from a
        // Mosaik executor, it will pick up the <link rel="mosaik" ...> entry
        return ModelAndView("nobrowser.html")
    }

    private fun HttpServletRequest.getServerUrl() =
        requestURL.toString().substringBefore(TOKENBURN_URI)


}

const val TOKENBURN_URI = "tokenburn"