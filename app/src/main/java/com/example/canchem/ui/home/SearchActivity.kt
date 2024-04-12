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
import android.os.Build

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
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                requestCameraPermission(takePictureIntent)
            }  else {
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
    //카메라 앱이 종료되고 다시 검색 액티비티로 돌아왔을 때 호출
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            // 여기서 촬영한 이미지를 처리하거나 표시할 수 있음.
            Toast.makeText(this, "사진을 찍었습니다!", Toast.LENGTH_SHORT).show()
        }
    }

    // 카메라 퍼미션을 요청하는 함수 
    private fun requestCameraPermission(intent: Intent) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    // 안드로이드 13 이전 버전 갤러리 퍼미션을 요청
    private fun requestGalleryPermission10(intent: Intent) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_GALLERY_PERMISSION)
        } else {
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
    }

    // 안드로이드 13 이상 버전 갤러리 퍼미션을 요청
    private fun requestGalleryPermission13(intent: Intent) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_GALLERY_PERMISSION)
        } else {
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
    }
}