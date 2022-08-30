package org.ergoplatform.mosaik.example.mosaik

import org.ergoplatform.mosaik.*
import org.ergoplatform.mosaik.model.MosaikApp
import org.ergoplatform.mosaik.model.ui.layout.HAlignment
import org.ergoplatform.mosaik.model.ui.layout.Padding
import org.ergoplatform.mosaik.model.ui.text.LabelStyle

fun MosaikApp.showMosaikVersionError(i: Int) {
    card(Padding.ONE_AND_A_HALF_DEFAULT) {
        column(Padding.ONE_AND_A_HALF_DEFAULT) {
            label(
                "This app needs a Mosaik executor capable of running Mosaik apps version $i\nPlease check for an update to your application or use the website.",
                style = LabelStyle.BODY1BOLD,
                textAlignment = HAlignment.CENTER
            )
        }
    }
}
