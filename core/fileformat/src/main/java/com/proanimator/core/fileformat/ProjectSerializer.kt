package com.proanimator.core.fileformat

import com.proanimator.core.engine.CanvasEngine
import com.proanimator.domain.model.Layer
import com.proanimator.domain.model.Stroke
import com.proanimator.domain.model.StrokePoint
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple JSON serializer for Phase 1.
 * Later this will evolve into the binary .pan format.
 */
object ProjectSerializer {

    fun toJson(engine: CanvasEngine, projectName: String = "Untitled"): String {
        val state = engine.getSerializableState()

        val root = JSONObject()
        root.put("version", 1)
        root.put("name", projectName)
        root.put("width", engine.width)
        root.put("height", engine.height)
        root.put("activeLayerId", state.activeLayerId)

        // Layers
        val layersArray = JSONArray()
        state.layers.forEach { layer ->
            val obj = JSONObject()
            obj.put("id", layer.id)
            obj.put("name", layer.name)
            obj.put("isVisible", layer.isVisible)
            obj.put("isLocked", layer.isLocked)
            obj.put("opacity", layer.opacity.toDouble())
            layersArray.put(obj)
        }
        root.put("layers", layersArray)

        // Strokes
        val strokesObj = JSONObject()
        state.strokesByLayer.forEach { (layerId, strokes) ->
            val strokeArray = JSONArray()
            strokes.forEach { stroke ->
                strokeArray.put(strokeToJson(stroke))
            }
            strokesObj.put(layerId, strokeArray)
        }
        root.put("strokes", strokesObj)

        return root.toString(2)
    }

    fun fromJson(json: String, engine: CanvasEngine) {
        val root = JSONObject(json)

        val layersArray = root.getJSONArray("layers")
        val layers = mutableListOf<Layer>()
        for (i in 0 until layersArray.length()) {
            val obj = layersArray.getJSONObject(i)
            layers.add(
                Layer(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    isVisible = obj.getBoolean("isVisible"),
                    isLocked = obj.optBoolean("isLocked", false),
                    opacity = obj.getDouble("opacity").toFloat()
                )
            )
        }

        val strokesObj = root.getJSONObject("strokes")
        val strokesByLayer = mutableMapOf<String, List<Stroke>>()

        val keys = strokesObj.keys()
        while (keys.hasNext()) {
            val layerId = keys.next()
            val strokeArray = strokesObj.getJSONArray(layerId)
            val strokes = mutableListOf<Stroke>()
            for (i in 0 until strokeArray.length()) {
                strokes.add(strokeFromJson(strokeArray.getJSONObject(i)))
            }
            strokesByLayer[layerId] = strokes
        }

        val activeLayerId = root.optString("activeLayerId", layers.firstOrNull()?.id ?: "")

        engine.loadState(
            CanvasEngine.EngineState(
                layers = layers,
                activeLayerId = activeLayerId,
                strokesByLayer = strokesByLayer
            )
        )
    }

    private fun strokeToJson(stroke: Stroke): JSONObject {
        val obj = JSONObject()
        obj.put("brushId", stroke.brushId)
        obj.put("color", stroke.color)
        obj.put("size", stroke.size.toDouble())
        obj.put("opacity", stroke.opacity.toDouble())

        val pointsArray = JSONArray()
        stroke.points.forEach { p ->
            val pObj = JSONObject()
            pObj.put("x", p.x.toDouble())
            pObj.put("y", p.y.toDouble())
            pObj.put("pressure", p.pressure.toDouble())
            pointsArray.put(pObj)
        }
        obj.put("points", pointsArray)
        return obj
    }

    private fun strokeFromJson(obj: JSONObject): Stroke {
        val pointsArray = obj.getJSONArray("points")
        val points = mutableListOf<StrokePoint>()
        for (i in 0 until pointsArray.length()) {
            val p = pointsArray.getJSONObject(i)
            points.add(
                StrokePoint(
                    x = p.getDouble("x").toFloat(),
                    y = p.getDouble("y").toFloat(),
                    pressure = p.optDouble("pressure", 1.0).toFloat()
                )
            )
        }

        return Stroke(
            points = points,
            brushId = obj.getString("brushId"),
            color = obj.getLong("color"),
            size = obj.getDouble("size").toFloat(),
            opacity = obj.optDouble("opacity", 1.0).toFloat()
        )
    }
}
