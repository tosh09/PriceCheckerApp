<template>
  <div>
    <!-- スキャナ部分 -->
    <BarcodeScanner @onDecode="handleScan" class="mb-4" />

    <!-- 取得結果表示部分 -->
    <div v-if="loading" class="text-center my-5">
      <div class="spinner-border text-accent-orange" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <p class="mt-2 text-muted">本番データを取得中...</p>
    </div>

    <!-- 結果カード -->
    <div v-if="productData && !loading" class="card-minimal p-4">
      <p class="text-center text-muted mb-1 small">バーコード: {{ productData.barcode }}</p>
      
      <div v-if="productData.errorMessage" class="alert alert-danger text-center">
        {{ productData.productName }}<br />
        <small>{{ productData.errorMessage }}</small>
      </div>

      <div v-else>
        <h5 class="fw-bold mb-4 text-center">{{ productData.productName }}</h5>
        
        <div class="row text-center mb-4">
          <div class="col-6 border-end border-1 opacity-75">
            <p class="mb-0 text-muted small">Amazon最安値</p>
            <h3 class="fw-bold mb-0">¥{{ displayPrice(productData.lowestPrice) }}</h3>
          </div>
          <div class="col-6">
            <p class="mb-0 text-muted small">Amazon現在価格</p>
            <h3 class="fw-bold mb-0 text-muted">¥{{ displayPrice(productData.currentPrice) }}</h3>
          </div>
        </div>

        <!-- 利益計算フォーム -->
        <div class="bg-offwhite p-3 rounded mb-3 border">
          <label class="form-label text-muted small mb-1 fw-bold">仕入金額 (¥)</label>
          <input type="number" class="form-control mb-3 border-accent-orange text-end fw-bold" v-model.number="costPrice" placeholder="0" />
          
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="text-muted small">販売手数料(10%)</span>
            <span class="text-muted">¥{{ commissionFee.toLocaleString() }}</span>
          </div>
          <hr class="my-2" />
          <div class="d-flex justify-content-between align-items-center">
            <span class="fw-bold text-darkgrey">想定利益</span>
            <h3 class="fw-bold mb-0" :class="profitColorClass">¥{{ calculatedProfit.toLocaleString() }}</h3>
          </div>
        </div>
      </div>
      
      <p class="text-center text-muted small mt-2">データ取得日時: {{ formatTime(productData.timestamp) }}</p>

      <button class="btn btn-minimal bg-accent-orange text-white w-100 mt-3 shadow-sm" @click="reset">次をスキャン</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'
import BarcodeScanner from '../components/BarcodeScanner.vue'

const productData = ref(null)
const loading = ref(false)
const costPrice = ref('') // UX向上:最初は空欄扱い

// Keepaの価格が100倍(例：25円が2500)で返ってきているかを推測する安全策（適宜修正）
// ここではデータが正しく「円」で来ていると仮定しつつ、変に大きい場合は直すロジックはあえて入れない
const normalizedPrice = computed(() => {
  if (!productData.value) return 0
  return productData.value.lowestPrice > 0 ? productData.value.lowestPrice : 0
})

// 販売手数料(10%)
const commissionFee = computed(() => {
  return Math.floor(normalizedPrice.value * 0.10)
})

// 想定利益
const calculatedProfit = computed(() => {
  const cost = Number(costPrice.value) || 0
  return normalizedPrice.value - cost - commissionFee.value
})

// 利益の色を変えるクラス
const profitColorClass = computed(() => {
  if (calculatedProfit.value > 0) return 'text-accent-orange'
  if (calculatedProfit.value < 0) return 'text-danger'
  return 'text-darkgrey'
})

const handleScan = async (barcodeData) => {
  if (loading.value) return;
  
  loading.value = true;
  productData.value = null;
  costPrice.value = ''; // UIリセット
  
  try {
    const response = await axios.get(`http://localhost:8080/api/v1/price/${barcodeData}`)
    productData.value = response.data
  } catch (error) {
    console.error("API通信エラー", error)
    alert("価格情報の取得に失敗しました。")
  } finally {
    loading.value = false;
  }
}

const displayPrice = (val) => {
  if (val < 0) return "-"
  return val.toLocaleString()
}

const reset = () => {
  productData.value = null
  costPrice.value = ''
}

const formatTime = (ts) => {
  if (!ts) return ''
  try {
    return new Date(ts).toLocaleString('ja-JP')
  } catch(e) { return ts }
}
</script>
