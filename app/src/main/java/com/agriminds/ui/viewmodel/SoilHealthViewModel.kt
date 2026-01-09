package com.agriminds.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agriminds.data.dao.SoilHealthDao
import com.agriminds.data.entity.SoilHealth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoilHealthViewModel @Inject constructor(
    private val soilHealthDao: SoilHealthDao
) : ViewModel() {

    private val _soilHealthRecords = MutableStateFlow<List<SoilHealth>>(emptyList())
    val soilHealthRecords: StateFlow<List<SoilHealth>> = _soilHealthRecords.asStateFlow()

    private val _recommendations = MutableStateFlow<String>("")
    val recommendations: StateFlow<String> = _recommendations.asStateFlow()

    fun loadSoilHealthRecords(farmerId: Int) {
        viewModelScope.launch {
            soilHealthDao.getSoilHealthByFarmer(farmerId).collect { records ->
                _soilHealthRecords.value = records
            }
        }
    }

    fun analyzeSoil(
        farmerId: Int,
        fieldName: String,
        phLevel: Double,
        nitrogen: Double,
        phosphorus: Double,
        potassium: Double
    ) {
        viewModelScope.launch {
            val recommendations = buildRecommendations(phLevel, nitrogen, phosphorus, potassium)
            
            val soilHealth = SoilHealth(
                farmerId = farmerId,
                fieldName = fieldName,
                phLevel = phLevel,
                nitrogen = nitrogen,
                phosphorus = phosphorus,
                potassium = potassium,
                recommendations = recommendations
            )

            soilHealthDao.insertSoilHealth(soilHealth)
            _recommendations.value = recommendations
        }
    }

    private fun buildRecommendations(
        phLevel: Double,
        nitrogen: Double,
        phosphorus: Double,
        potassium: Double
    ): String {
        val builder = StringBuilder()
        
        builder.append("**Soil Analysis Results:**\n\n")
        
        // pH Analysis
        builder.append("**pH Level: $phLevel**\n")
        when {
            phLevel < 6.0 -> builder.append("• Acidic soil. Add lime (200-300 kg/acre) to raise pH.\n")
            phLevel > 7.5 -> builder.append("• Alkaline soil. Add sulfur or organic matter to lower pH.\n")
            else -> builder.append("• Optimal pH range for most crops.\n")
        }
        builder.append("\n")
        
        // NPK Analysis
        builder.append("**NPK Levels:**\n\n")
        
        builder.append("**Nitrogen: ${nitrogen}%**\n")
        when {
            nitrogen < 1.5 -> builder.append("• Low. Apply urea (50-75 kg/acre) or compost.\n")
            nitrogen > 3.0 -> builder.append("• High. Reduce nitrogen fertilizer application.\n")
            else -> builder.append("• Adequate for most crops.\n")
        }
        builder.append("\n")
        
        builder.append("**Phosphorus: ${phosphorus}%**\n")
        when {
            phosphorus < 0.5 -> builder.append("• Low. Add TSP fertilizer or bone meal.\n")
            phosphorus > 1.5 -> builder.append("• Adequate to high.\n")
            else -> builder.append("• Adequate for crop growth.\n")
        }
        builder.append("\n")
        
        builder.append("**Potassium: ${potassium}%**\n")
        when {
            potassium < 0.5 -> builder.append("• Low. Apply MOP (Muriate of Potash) fertilizer.\n")
            potassium > 1.5 -> builder.append("• Adequate to high.\n")
            else -> builder.append("• Adequate for crop requirements.\n")
        }
        builder.append("\n")
        
        builder.append("**General Recommendations:**\n")
        builder.append("• Conduct soil tests annually before planting season\n")
        builder.append("• Add organic matter (compost/cow dung) regularly\n")
        builder.append("• Practice crop rotation to maintain soil fertility\n")
        builder.append("• Ensure proper drainage to prevent waterlogging\n")
        
        return builder.toString()
    }
}
