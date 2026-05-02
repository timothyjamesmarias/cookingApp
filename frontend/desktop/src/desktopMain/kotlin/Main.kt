import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.timothymarias.cookingapp.shared.App
import com.timothymarias.cookingapp.shared.di.initSeeding
import com.timothymarias.cookingapp.shared.di.sharedModule
import org.koin.core.context.startKoin

fun main() = application {
    val koin = startKoin { modules(sharedModule()) }.koin
    initSeeding(koin)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Cooking App"
    ) {
        App()
    }
}
