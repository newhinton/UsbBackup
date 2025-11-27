package de.felixnuesse.usbbackup

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class CustomizableCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private var top = 0f
    private var bottom = 0f

    private fun updateBackground() {
        val shapeModel = ShapeAppearanceModel.builder()
            .setTopLeftCornerSize(top)
            .setTopRightCornerSize(top)
            .setBottomRightCornerSize(bottom)
            .setBottomLeftCornerSize(bottom)
            .build()

        val drawable = MaterialShapeDrawable(shapeModel).apply {
            fillColor = cardBackgroundColor
            elevation = this@CustomizableCardView.cardElevation
        }

        // Replace CardView’s background
        background = drawable
    }

    fun setTopCorner(
        top: Float
    ) {
        this.top = top
        updateBackground()
    }


    fun setBottomCorner(
        bottom: Float
    ) {
        this.bottom = bottom
        updateBackground()
    }
}