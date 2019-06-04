package pe.chalk.ffff

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors

@Serializable
data class Response(@SerialName("JSON") val tickets: List<Ticket>)

@Serializable
data class Ticket(
    @SerialName("SeatGrade") val index: Int,
    @SerialName("SeatGradeName") val name: String,
    @SerialName("RemainCnt") val count: Int,
    @SerialName("SalesPrice") val price: Int
)

@UnstableDefault
class Main : Application() {
    private val encoding = Charset.forName("EUC-KR")
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private val favicon = "https://img.finalfantasyxiv.com/lds/h/9/_Huf58epDlt9vXiO8IIfPXxtXI.png"
    private val referer = "http://ticket.interpark.com/Ticket/Goods/GoodsInfo.asp?GoodsCode=19007681&pis1=ticket&pis2=product"
    private val endpoint = "http://ticket.interpark.com/Ticket/Goods/GoodsInfoJSON.asp?Flag=RemainSeat&GoodsCode=19007681&PlaceCode=11011096&PlaySeq=001&Callback=fnPlaySeqChangeCallBack"

    private fun random(): Background {
        val color = "hsl(${(0..360).random()}, 100%, 100%)" // why
        return Background(BackgroundFill(Color.web(color), null, null))
    }

    private fun fetch(): Array<Int> {
        val con = URL(endpoint).openConnection()
        con.addRequestProperty("Referer", referer)

        val res = con.inputStream.bufferedReader(encoding).lines().collect(Collectors.joining())
        val tickets = Json.nonstrict.parse(Response.serializer(), res.drop(24).dropLast(2)).tickets

        return arrayOf(
            tickets.find { it.name.startsWith("일반") }!!.count,
            tickets.find { it.name.startsWith("우선") }!!.count
        )
    }

    override fun start(stage: Stage) {
        var x = 0.0
        var y = 0.0

        val time = Label()
        val status = Label()

        val root = VBox()
        root.alignment = Pos.CENTER
        root.children.addAll(time, status)
        root.setOnMousePressed { x = stage.x - it.screenX; y = stage.y - it.screenY }
        root.setOnMouseDragged { stage.x = it.screenX + x; stage.y = it.screenY + y }

        stage.title = "팬페 예매현황"
        stage.isAlwaysOnTop = true
        stage.icons.add(Image(favicon))
        stage.initStyle(StageStyle.UNDECORATED)
        stage.scene = Scene(root, 128.0, 50.0)
        stage.show()

        Thread {
            while (stage.isShowing) {
                val t = fetch()
                Platform.runLater {
                    time.text = formatter.format(Date())
                    status.text = "일반 ${t[0]} / 우선 ${t[1]}"
                    root.background = if (t[1] > 0) {
                        stage.toFront(); random()
                    } else null
                }
                Thread.sleep(333)
            }
        }.start()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java, *args)
        }
    }
}
