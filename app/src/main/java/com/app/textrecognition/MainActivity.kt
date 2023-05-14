package com.app.textrecognition

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.textrecognition.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageView: ImageView
    private var imageBitmap: Bitmap? = null
    private var imageUri: Uri? = null
    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageView = binding.image

        if(allPermissionsGranted()){
            Log.d(TAG, "All permission granted")
        } else{
            requestPermissions()
        }

        binding.captureImage.setOnClickListener {
            takeImage()
        }

        binding.processImage.setOnClickListener {
            processImage()
        }
    }

    private fun requestPermissions(){
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun takeImage(){
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Action")
        builder.setItems(options) { _, item ->
            when {
                options[item] == "Take Photo" -> {
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (takePictureIntent.resolveActivity(packageManager) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
                options[item] == "Choose from Gallery" -> {
                    val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK)
                }
            }
        }
        builder.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val extras = data?.extras
                    imageBitmap = extras?.get("data") as Bitmap
                    imageUri = null
                    imageView.setImageBitmap(imageBitmap)
                    binding.resultText.text = null
                }
                REQUEST_IMAGE_PICK -> {
                    imageUri = data?.data
                    imageBitmap = null
                    imageView.setImageURI(imageUri)
                    binding.resultText.text = null
                }
            }
        }
    }

    private fun processImage(){
        var image: InputImage? = null

        if (imageBitmap!=null) {
            image = imageBitmap?.let {
                InputImage.fromBitmap(it, 0)
            }
        }
        else if(imageUri!=null) {
            image = imageUri?.let {
                InputImage.fromFilePath(this, it)
            }
        }
        else{
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show()
        }
        recognizeText(image)
    }

    private fun recognizeText(image: InputImage?){
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        image?.let {
            textRecognizer.process(it)
                .addOnSuccessListener { visionText ->
                    binding.resultText.text = visionText.text
                    Toast.makeText(this, "OCR completed", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
        }
    }
    companion object{
        private const val TAG  = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 10
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
        private val REQUIRED_PERMISSIONS = mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ).apply {}.toTypedArray()
    }
}