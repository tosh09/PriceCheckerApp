<template>
  <div class="scanner-container card-minimal p-3 text-center">
    <div v-if="!isScanning" class="py-5">
      <p class="text-muted mb-2">カメラでバーコードを読み取ります</p>
      
      <!-- 開発中のテストボタン追加 -->
      <div class="d-flex justify-content-center gap-2 mb-4">
        <button @click="startScan" class="btn btn-dark rounded-pill px-4">カメラ起動</button>
        <button @click="sendTestBarcode" class="btn btn-outline-secondary rounded-pill px-3">テスト用データ送信</button>
      </div>
    </div>
    
    <div v-else>
      <div id="reader" style="width: 100%; max-width: 400px; margin: 0 auto; border-radius: 8px; overflow: hidden; background: #000;"></div>
      <button @click="stopScan" class="btn btn-outline-danger btn-sm mt-3 rounded-pill">スキャンキャンセル</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode'

const emit = defineEmits(['onDecode'])
const isScanning = ref(false)
let html5QrCode = null;

const startScan = async () => {
  isScanning.value = true
  
  setTimeout(async () => {
    try {
      // ✅ 修正1: フォーマットを日本の商品用（JANコード = EAN_13 / EAN_8）に限定して認識精度を上げる
      const config = {
        formatsToSupport: [
          Html5QrcodeSupportedFormats.EAN_13,
          Html5QrcodeSupportedFormats.EAN_8
        ]
      };
      
      html5QrCode = new Html5Qrcode("reader", config)
      
      await html5QrCode.start(
        { facingMode: "environment" },
        { fps: 10, qrbox: { width: 250, height: 100 } }, // 横長のスキャン枠
        (decodedText, decodedResult) => {
          // 読み取り成功！
          stopScan()
          emit('onDecode', decodedText)
        },
        (errorMessage) => {
           // 読み取り中は常にこのエラー（まだ見つかりません等）が出るので無視
        })
    } catch (err) {
      console.error("カメラ起動エラー: ", err)
      alert("カメラの起動に失敗しました。ブラウザのカメラアクセス許可を確認してください。")
      isScanning.value = false
    }
  }, 100)
}

const stopScan = async () => {
  if (html5QrCode) {
    try {
       await html5QrCode.stop()
       html5QrCode.clear()
    } catch(err) {
       console.log("カメラ停止エラー", err)
    }
  }
  isScanning.value = false
}

// ✅ 修正2: 開発モード時（PCなど）にカメラを使わずテストするボタン
const sendTestBarcode = () => {
  // 適当なダミーのJANコードをフロントからバックエンドへ流す
  emit('onDecode', "841710106626")
}

onBeforeUnmount(() => {
  stopScan()
})
</script>
