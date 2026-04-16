package com.example.pricechecker.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.zip.GZIPInputStream

@RestController
@RequestMapping("/api/v1/price")
class PriceController(private val restTemplate: RestTemplate = RestTemplate()) {

    private val logger = LoggerFactory.getLogger(PriceController::class.java)

    @Value("\${keepa.api.key}")
    lateinit var keepaApiKey: String

    // アプリ側へ返すためのデータクラス
    data class PriceResponse(
        val barcode: String,
        val productName: String,
        val currentPrice: Int,
        val lowestPrice: Int,
        val platform: String,
        val timestamp: String,
        val errorMessage: String? = null
    )

    @GetMapping("/{barcode}")
    fun getPrice(@PathVariable barcode: String): PriceResponse {
        logger.info("価格取得リクエストを受信しました。バーコード={}", barcode)
        
        try {
            // Keepa API呼び出し
            val url = "https://api.keepa.com/product?key=$keepaApiKey&domain=5&code=$barcode"
            logger.info("Keepa APIへ通信を開始します...")
            
            // GZIPの強制圧縮対策：バイナリ(ByteArray)として安全に受け取る
            val rawBytes = restTemplate.getForObject(url, ByteArray::class.java)
            if (rawBytes == null) throw Exception("KeepaAPIからのレスポンスが空です")
            
            // GZIP圧縮されているかマジックナンバー（1F 8B）で判定して解凍
            val rawJson: String
            if (rawBytes.size >= 2 && rawBytes[0] == 0x1F.toByte() && rawBytes[1] == 0x8B.toByte()) {
                logger.info("GZIP圧縮を検知しました。解凍処理を実行します。")
                GZIPInputStream(ByteArrayInputStream(rawBytes)).bufferedReader().use {
                    rawJson = it.readText()
                }
            } else {
                logger.info("非圧縮データを受信しました。")
                rawJson = String(rawBytes, Charsets.UTF_8)
            }
            
            logger.info("Keepa APIからレスポンス取得成功。解析を実行します。")
            // デバッグログ（巨大なので先頭だけ表示）
            logger.info("レスポンス先頭: {}", rawJson.take(150))

            // 「商品の空振り」チェック
            if (rawJson.contains("\"products\": []") || !rawJson.contains("\"products\"")) {
                logger.info("商品が見つかりませんでした。エラー応答")            
                return PriceResponse(barcode, "商品が見つかりませんでした", 0, 0, "Amazon(Keepa)", LocalDateTime.now().toString(), "Not Found")
            }

            // --- 強制的な手動パース（正規表現による抽出） ---
            logger.info("商品情報のパース処理開始します。")
            
            // 1. 商品名 (title)
            val titleRegex = """"title"\s*:\s*"([^"]+)"""".toRegex()
            val titleMatch = titleRegex.find(rawJson)
            val title = titleMatch?.groups?.get(1)?.value ?: "商品名不明(手動抽出)"

            // 2. 価格 (stats.current配列)
            val currentRegex = """"current"\s*:\s*\[([^\]]+)\]""".toRegex()
            val currentMatch = currentRegex.find(rawJson)
            
            logger.info("商品情報の価格パース完了。")
            var cPrice = -1
            if (currentMatch != null) {
                val csvStr = currentMatch.groups[1]?.value ?: ""
                val priceTokens = csvStr.split(",")
                
                // 配列（Amazon, 新品, 中古...）の中から、最初に出現した「有効な価格（>0）」を採用する
                for (token in priceTokens) {
                    val price = token.trim().toIntOrNull() ?: -1
                    if (price > 0) {
                        cPrice = price
                        break
                    }
                }
            }
            
            val response = PriceResponse(
                barcode = barcode,
                productName = title,
                currentPrice = cPrice,
                lowestPrice = cPrice, // Phase2初版では単純化のため現在価格＝最安値
                platform = "Amazon",
                timestamp = LocalDateTime.now().toString()
            )
            
            logger.info("レスポンスを返却します: {}", response)
            return response
            
        } catch (e: Exception) {
            logger.error("例外発生", e)
            return PriceResponse(barcode, "エラーが発生しました", 0, 0, "Error", LocalDateTime.now().toString(), e.message)
        }
    }
}
