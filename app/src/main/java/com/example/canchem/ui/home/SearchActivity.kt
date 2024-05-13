package com.example.canchem

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.canchem.databinding.ActivitySearchBinding
import androidx.appcompat.widget.SearchView
import android.text.InputFilter
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.AlertDialog
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.soundcloud.android.crop.Crop
import java.io.ByteArrayOutputStream
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class SearchActivity : AppCompatActivity() {
    companion object{
        //카메라 앱으로 사진을 촬영하기 위한 요청 코드
        const val REQUEST_IMAGE_CAPTURE = 1
        //카메라 앱으로 사진을 촬영한 후의 요청 코드 (결과를 식별하는데 사용)
        const val REQUEST_CAMERA_PERMISSION = 2
        //갤러리 앱에서 사진을 선택하기 위한 요청 코드
        const val REQUEST_GALLERY_PERMISSION =3
        // 갤러리 앱에서 이미지를 선택한 후의 요청 코드 (결과를 식별하는데 사용)
        const val REQUEST_IMAGE_PICK = 4
    }

    // 카메라로 촬영된 이미지의 파일 경로를 저장하는 프로퍼티
    private var imageUri: Uri? = null
    private var mCurrentPhotoPath: String? =null

    // Bitmap을 Base64로 변환하는 함수
    private fun bitmapToBase64(croppedBitmap: Bitmap?): String {
        croppedBitmap?.let {
            val byteArrayOutputStream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
        return ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // EditText 가져오기
        val editText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        // 입력 필터링 설정
        editText.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val allowedChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ=()-#.$:/\\"
            val filteredStringBuilder = StringBuilder()
            // 입력된 문자 중에서 허용된 문자인지 확인
            for (i in start until end) {
                if (allowedChars.contains(source[i])) {
                    filteredStringBuilder.append(source[i])
                }
                else{
                    //허용되지 않는 문자가 입력되었을 때 토스트 메시지 표시
                    Toast.makeText(this, "잘못된 입력 방식입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            // 필터링된 문자열 반환
            filteredStringBuilder.toString()
        })

        binding.searchView.maxWidth = Int.MAX_VALUE
        binding.searchView.isSubmitButtonEnabled = true

        // 검색 버튼 클릭 이벤트 처리
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // 검색 버튼이 클릭되었을 때 실행됩니다.
                // 여기서는 ApiActivity로의 인텐트 전환을 수행합니다.
                val intent = Intent(this@SearchActivity, ApiActivity::class.java)
                // 검색어를 인텐트에 추가.
                intent.putExtra("search_query", query)
                startActivity(intent)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // 검색어가 변경될 때 실행됩니다.
                return false
            }
        })

        // 카메라 버튼 클릭시 카메라 앱 실행
        binding.cameraButton.setOnClickListener {
            val state = Environment.getExternalStorageState()

            //외장 메모리 검사
            if(Environment.MEDIA_MOUNTED == state){
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                if(takePictureIntent.resolveActivity(packageManager) != null){
                    var photoFile: File? = null

                    try{
                        photoFile = createImageFile()
                    } catch (ex: IOException){
                        Toast.makeText(this, "이미지 처리 오류. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    if(photoFile != null){
                        val providerURI = FileProvider.getUriForFile(this, "com.example.canchem.fileprovider", photoFile)

//                        // 파일 URI가 올바르게 설정되었는지 토스트 메시지로 확인
//                        if (providerURI != null) {
//                            Toast.makeText(this, "File URI: $providerURI", Toast.LENGTH_SHORT).show()
//                        } else {
//                            Toast.makeText(this, "Failed to get file URI", Toast.LENGTH_SHORT).show()
//                        }

                        imageUri = providerURI

                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI)
                        requestCameraPermission(takePictureIntent)
                    }
                }
            } else {
                Toast.makeText(this, "카메라 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 갤러리 버튼 클릭시 갤러리 앱 실행
        binding.galleryButton.setOnClickListener {
            val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val sdkVersion = Build.VERSION.SDK_INT
            pickPhotoIntent.type = "image/*"
            if (pickPhotoIntent.resolveActivity(packageManager) != null) {
                // Android 13이상 버전 퍼미션을 요청
                if (sdkVersion >= 33) {
                    requestGalleryPermission13(pickPhotoIntent)
                }
                // Android 13이전 버전 퍼미션을 요청
                else {
                    requestGalleryPermission10(pickPhotoIntent)
                }
            }
            else {
                Toast.makeText(this, "갤러리 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    }
    // 카메라나 갤러리 앱을 실행한 후 액티비티로 돌아왔을 때
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        when (requestCode) {
            // 갤러리에서 이미지를 선택한 경우
            REQUEST_IMAGE_PICK -> {
                val photoUri = data?.data
//                photoUri?.let{showConfirmationDialog(it)}
                photoUri?.let { cropImage(it) }
            }
            // 카메라에서 이미지를 선택한 경우
            REQUEST_IMAGE_CAPTURE -> {
                val photoUri = imageUri

                photoUri?.let { cropImage(it) }
            }
            // 이미지를 크롭 한 후
            Crop.REQUEST_CROP -> {
                val result = Crop.getOutput(data)
                result?.let { croppedUri ->
                    // 크롭된 이미지의 Uri를 통해 Bitmap으로 변환
                    val croppedBitmap =
                        MediaStore.Images.Media.getBitmap(contentResolver, croppedUri)
                    // Bitmap을 Base64로 변환
                    val base64Image = bitmapToBase64(croppedBitmap)

                    // 서버로 업로드 등 작업 수행
                }
            }
        }
    }

//        //사진 선택 확인 다이얼로그 표시 함수
//        private fun showConfirmationDialog(photoUri: Uri) {
//            val alertDialogBuilder = AlertDialog.Builder(this)
//            alertDialogBuilder.apply {
//                setMessage("선택한 사진을 사용하시겠습니까?")
//                setPositiveButton("확인") { dialog, which ->
//                    // 확인 버튼을 누르면 크롭 화면으로 이동
//                    cropImage(photoUri)
//                }
//                setNegativeButton("취소") { dialog, which ->
//                    // 취소 버튼을 누르면 다이얼로그를 닫고 다시 갤러리 실행
//                    dialog.dismiss()
//                }
//            }
//            val alertDialog = alertDialogBuilder.create()
//            alertDialog.show()
//        }

    // 이미지를 Crop 화면으로 보내주는 함수
    private fun cropImage(photoUri: Uri) {
        // 임시 파일로 저장할 파일 객체 생성
        val savingUri = Uri.fromFile(createTempFile("cropImage", ".jpg"))
        Crop.of(photoUri, savingUri).withAspect(1, 1).start(this)
    }

    // 카메라 퍼미션을 요청하는 함수
    private fun requestCameraPermission(intent: Intent) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    // 안드로이드 13 이전 버전 갤러리 퍼미션을 요청
    private fun requestGalleryPermission10(intent: Intent) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_GALLERY_PERMISSION
            )
        } else {
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
    }

    // 안드로이드 13 이상 버전 갤러리 퍼미션을 요청
    private fun requestGalleryPermission13(intent: Intent) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                REQUEST_GALLERY_PERMISSION
            )
        } else {
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }d
    }

    // 카메라로 찍은 사진을 저장할 임시파일 생성 함수
    private fun createImageFile(): File {
        // 이미지 파일 이름 생성
        val timeStamp = SimpleDateFormat("yyyyMMdd").format(Date())
        val imageFileName = "CanChem_${timeStamp}.jpg" //ex) CanChem_20240509.jpg

        var imageFile: File? = null
        val storageDir = File(
            Environment.getExternalStorageDirectory().toString() + "/Pictures",
            "pic"
        )

        // 저장 디렉토리가 존재하지 않으면 생성
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        imageFile = File(storageDir, imageFileName)
        mCurrentPhotoPath = imageFile.absolutePath

//            // 파일이 생성되었는지 토스트 메시지로 확인
//            if (imageFile.exists()) {
//                Toast.makeText(this, "File created at: ${imageFile.absolutePath}", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show()
//            }

        return imageFile
    }
}
