package com.example.kidosart

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.View
import android.widget.Gallery
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialogue_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint : ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(10.toFloat())

        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_select))


        ib_brush.setOnClickListener { showBrushSizeChooseDialogue() }

        ib_gallery.setOnClickListener{
            if (isReadStorageAllowed())
            {
                val pickPhotoIntent = Intent(Intent.ACTION_PICK , MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoIntent , GALLERY)


            }
            else
            {
                requestStoragePermission()
            }
        }

        ib_undo.setOnClickListener { drawing_view.onClickUndo() }

        ib_redo.setOnClickListener { drawing_view.onClickRedo() }

        ib_save.setOnClickListener{
            if (isReadStorageAllowed())
            {
                BitmapAsyncTask(getBitMapFromView(fl_drawing_view_container)).execute()
            }
            else
            {
                requestStoragePermission()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == GALLERY)
            {
                try
                {
                    if (data!!.data !=null)
                    {
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    }
                    else
                    {
                        Snackbar.make(drawing_view,"Error in parsing the image \n Or, its corrupted", Snackbar.LENGTH_SHORT).show()
                    }
                }
                catch (e:Exception)
                {
                    e.printStackTrace()
                }
            }
        }

    }

    private fun showBrushSizeChooseDialogue()
    {
        val brushType = Dialog(this)
        brushType.setContentView(R.layout.dialogue_brush_size)
        brushType.setTitle("Brush Size : ")
        val smallBtn = brushType.ib_small_brush
        smallBtn.setOnClickListener {
            drawing_view.setSizeForBrush(10.toFloat())
            brushType.dismiss()
        }
        val mediumBtn = brushType.ib_medium_brush
        mediumBtn.setOnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushType.dismiss()
        }
        brushType.ib_large_brush.setOnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushType.dismiss()
        }
        brushType.show()
    }

    fun paintClicked(view : View)
    {
        if (view != mImageButtonCurrentPaint)
        {
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()

            drawing_view.setColor(colorTag)

            imageButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_select))
            mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this , R.drawable.pallet_normal))

            mImageButtonCurrentPaint = view

        }
    }

    companion object
    {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }

    private fun requestStoragePermission()
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE).toString()))
        {
            Toast.makeText(this , "You need permission to add background" , Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this , arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE) , STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE)
        {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this@MainActivity , "Permission granted now \n You can read the Storage files." , Toast.LENGTH_SHORT).show()

            }
            else
            {
                //Toast.makeText(this@MainActivity , "Permission denied !" , Toast.LENGTH_SHORT).show()
                Snackbar.make(drawing_view,"Permission denied !",Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun isReadStorageAllowed() : Boolean
    {
        val result = ContextCompat.checkSelfPermission(this , android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitMapFromView(view: View):Bitmap
    {
        val returnedBitmap = Bitmap.createBitmap(view.width , view.height , Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable !=null)
        {
            bgDrawable.draw(canvas)
        }
        else
        {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap : Bitmap) : AsyncTask<Any , Void , String>()
    {
        private lateinit var mProgressBar : Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialogue()
        }

        override fun doInBackground(vararg params: Any?): String {

            var result =""

            if (mBitmap != null)
            {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,100,bytes)

                    val file = File(externalCacheDir!!.absoluteFile.toString() + File.separator + "KidosArt_" + System.currentTimeMillis() / 1000 + ".png")

                    val fileOutputStream = FileOutputStream(file)
                    fileOutputStream.write(bytes.toByteArray())
                    fileOutputStream.close()

                    result = file.absolutePath
                }
                catch (e:Exception)
                {
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialoge()
            if (!result!!.isEmpty())
            {
                val mySnackbar = Snackbar.make(fl_drawing_view_container , "File Saved Successfully at $result" ,Snackbar.LENGTH_SHORT).setActionTextColor(Color.WHITE)
                mySnackbar.show()
            }
            else
            {
               val mySnackbar = Snackbar.make(fl_drawing_view_container , "Something went wrong! \n Please try again." , Snackbar.LENGTH_SHORT).setActionTextColor(Color.RED)
                mySnackbar.show()
            }

            MediaScannerConnection.scanFile(this@MainActivity , arrayOf(result) , null)
            {
                path, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND.apply{(shareIntent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)
                    shareIntent.type = "image/*"
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(shareIntent , "Share"))
            }

        }

        private fun showProgressDialogue()
        {
            mProgressBar = Dialog(this@MainActivity )
            mProgressBar.setContentView(R.layout.dialogue_custom_progress)
            mProgressBar.show()
        }

        private fun cancelProgressDialoge()
        {
            mProgressBar.dismiss()
        }

    }

}
