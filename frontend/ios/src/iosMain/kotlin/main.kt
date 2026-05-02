import androidx.compose.ui.window.ComposeUIViewController
import com.timothymarias.cookingapp.shared.App
import com.timothymarias.cookingapp.shared.di.initSeeding
import com.timothymarias.cookingapp.shared.di.sharedModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val koin = startKoin { modules(sharedModule()) }.koin
    initSeeding(koin)
    return ComposeUIViewController { App() }
}
