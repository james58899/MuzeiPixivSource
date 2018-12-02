package one.oktw.muzeipixivsource.util

import android.content.Context
import android.graphics.Bitmap
import androidx.renderscript.Allocation.createFromBitmap
import androidx.renderscript.Allocation.createTyped
import androidx.renderscript.Element
import androidx.renderscript.RenderScript
import androidx.renderscript.ScriptIntrinsicResize
import androidx.renderscript.Type
import one.oktw.muzeipixivsource.renderscript.ScriptC_Gradient
import one.oktw.muzeipixivsource.renderscript.ScriptC_RAISR
import kotlin.math.max

object RAISR {
    private const val PATCH_SIZE = 11
    private const val GRADIENT_SIZE = 9
    private const val Q_ANGLE = 24
    private const val Q_STRENGTH = 3
    private const val Q_COHERENCE = 3

    private val MARGIN = max(PATCH_SIZE, GRADIENT_SIZE) / 2

    fun test(context: Context, image: Bitmap): Bitmap {
        val rs = RenderScript.create(context)
        val raisr = ScriptC_RAISR(rs)

        val origin = createFromBitmap(rs, image)
        val upscale = createTyped(rs, Type.createXY(rs, origin.element, origin.type.x * 2, origin.type.y * 2))

        // upscale
        ScriptIntrinsicResize.create(rs).apply {
            setInput(origin)
            forEach_bicubic(upscale)
        }

        // free origin image allocation
        origin.destroy()

//            raisr.invoke_process(upscale)

        // only process light
        val grayed = createTyped(rs, Type.createXY(rs, Element.U8(rs), upscale.type.x, upscale.type.y))
            .also { raisr.forEach_toGray(upscale, it) }

        val patchBlock = createTyped(rs, Type.createXY(rs, Element.U8(rs), PATCH_SIZE, PATCH_SIZE))
        val gradientBlock = createTyped(rs, Type.createXY(rs, Element.F32(rs), GRADIENT_SIZE, GRADIENT_SIZE))

        for (x in MARGIN..grayed.type.x - MARGIN) {
            for (y in MARGIN..grayed.type.y - MARGIN) {
                patchBlock.copy2DRangeFrom(0, 0, PATCH_SIZE, PATCH_SIZE, grayed, x - PATCH_SIZE / 2, y - PATCH_SIZE / 2)
                patchBlock.copy2DRangeFrom(0, 0, GRADIENT_SIZE, GRADIENT_SIZE, grayed, x - GRADIENT_SIZE / 2, y - GRADIENT_SIZE / 2)
            }
        }

        // calculation gradient
        val gradientX = createTyped(rs, Type.createXY(rs, Element.F32(rs), upscale.type.x, upscale.type.y))
        val gradientY = createTyped(rs, Type.createXY(rs, Element.F32(rs), upscale.type.x, upscale.type.y))

        val gradient = ScriptC_Gradient(rs)
        gradient.invoke_gradient(grayed, gradientX, gradientY)

        // create patch
        val patch = createTyped(rs, Type.createXY(rs, Element.I16(rs), upscale.type.x, upscale.type.y))
        raisr.forEach_genPatch(grayed, gradientX, gradientY, patch)

        grayed.destroy()
        gradientX.destroy()
        gradientY.destroy()

        raisr.forEach_applyPatch(upscale, patch, upscale)

        patch.destroy()

        return Bitmap.createBitmap(upscale.type.x, upscale.type.y, Bitmap.Config.ARGB_8888)
            .apply(upscale::copyTo)
            .also { rs.destroy() }
    }
}
