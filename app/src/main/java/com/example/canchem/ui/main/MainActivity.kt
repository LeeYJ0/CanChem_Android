package com.example.canchem.ui.main

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.canchem.R
import com.example.canchem.SearchActivity
import com.example.canchem.data.source.GoogleLoginInterface
import com.example.canchem.data.source.GoogleToken
import com.example.canchem.data.source.NaverLoginInterface
import com.example.canchem.data.source.NaverToken
import com.example.canchem.data.source.SignoutInterface
import com.example.canchem.data.source.Token
import com.example.canchem.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* View Binding 설정 */
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        // retrofit 변수 생성
//        val retrofit = Retrofit.Builder()
//            .baseUrl("http://localhost:8080")
//            .addConverterFactory(GsonConverterFactory.create()) //kotlin to json(역 일수도)
//            .build()
//
//        // retrofit객체 생성
//        val loginService = retrofit.create(LoginInterface::class.java)
//
////        val data = LogIn("12345") //여기 네이버에서 준 정보 넣으면 됨.
//        val call = loginService.getLoginToken(something)
//        call.enqueue(object : Callback<NaverToken> {
//            override fun onResponse(call: Call<NaverToken>, response: Response<NaverToken>) { //요청성공시
//                if (response.isSuccessful) {
//                    Toast.makeText(this@MainActivity, "성공", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this@MainActivity, "실패", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<NaverToken>, t: Throwable) { //요청실패시
//                Toast.makeText(this@MainActivity, "아예 실패", Toast.LENGTH_SHORT).show()
//            }
//        })

        enableEdgeToEdge()  // 상태표시줄 투명하게 만듦

        /* 네아로 SDK 객체 초기화 */
        val naverClientId = getString(R.string.naver_client_id) // 발급 받은 naver client id 값
        val naverClientSecret = getString(R.string.naver_client_secret) // 발급 받은 naver client secret 값
        val naverClientName = getString(R.string.naver_client_name) // 어플 이름
        NaverIdLoginSDK.initialize(this, naverClientId, naverClientSecret , naverClientName)    // 네아로 객체 초기화

        googleAuthLauncher()    // Google 로그인 초기화
        googleSignInClient = getGoogleClient()  // Google Client 초기화
        

        /* Google 로그인 버튼 클릭*/
        binding.btnGoogleLogin.setOnClickListener {
            resultLauncher.launch(googleSignInClient.getSignInIntent())  // 구글 로그인 창으로 넘어감
        }

        /* 네이버 로그인 버튼 클릭 */
        binding.btnNaverLogin.setOnClickListener {

            /* 네이버 Access Token 받기 */
            val oauthLoginCallback = object : OAuthLoginCallback {
                override fun onSuccess() {
                    // 네이버 로그인 인증이 성공했을 때 수행할 코드 추가
                    val accessToken = NaverIdLoginSDK.getAccessToken().toString()   // 접근 토큰
                    val refreshToken = NaverIdLoginSDK.getRefreshToken().toString() // 갱신 토큰
                    val expiresAt = NaverIdLoginSDK.getExpiresAt().toString()   // 만료 기한 (초)
                    val type = NaverIdLoginSDK.getTokenType().toString()    // 토큰 타입
                    val state = NaverIdLoginSDK.getState().toString()   // 로그인 인스턴트의 현재 상태

                    Toast.makeText(this@MainActivity, "Access Token : ${accessToken}\n" +
                            "Refresh Token : ${refreshToken}\n" +
                            "Expires at : ${expiresAt}\n" +
                            "Type : ${type}\n" +
                            "State : ${state}", Toast.LENGTH_SHORT).show()

                    NidOAuthLogin().callProfileApi(nidProfileCallback)

                    val intent = Intent(this@MainActivity, SearchActivity::class.java)
                    startActivity(intent)
                }
                override fun onFailure(httpStatus: Int, message: String) {
                    val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                    val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                    Toast.makeText(this@MainActivity,"errorCode:$errorCode, errorDesc:$errorDescription",Toast.LENGTH_SHORT).show()
                }
                override fun onError(errorCode: Int, message: String) {
                    onFailure(errorCode, message)
                }
            }
            NaverIdLoginSDK.authenticate(this, oauthLoginCallback)  // 토큰 가져오기
        }

        /* 로그아웃 버튼 클릭 */
        binding.btnNaverLogout.setOnClickListener {
            naverLogout()   // 네이버 로그아웃
            googleLogout()  // 구글 로그아웃
        }

        /* 탈퇴 버튼 클릭 */
        binding.btnNaverDelete.setOnClickListener {
            naverDeleteToken()  // 네이버 연동 해제
            googleDeleteToken()  // 구글 연동 해제
        }
    }

    val first : String = "Baerer"
    lateinit var googleSignInClient : GoogleSignInClient
    lateinit var resultLauncher : ActivityResultLauncher<Intent>
//    private lateinit var logInService: LogInService // retrofit


    /* Retrofit Iterface */
//    interface LogInService{
//        @GET("/api/login/naver")
//        fun getLoginToken(
//            @Query("id") data : String
//        ) : Call<Void>
//    }

    //firebase DB
//    val firebaseUsers = FirebaseDatabase.getInstance().getReference("Users")
//    val firebaseUsersToken = firebaseUsers.child("Token")
    val database = Firebase.database
    val tokenInFirebase = database.getReference("Token")

//    //interceptor
//    fun provideOkHttpClient(
//        AccessTokenInterceptor: AccessTokenInterceptor
//    ): OkHttpClient {
//        return OkHttpClient.Builder()
//            .addNetworkInterceptor(AccessTokenInterceptor)
//            .build()
//    }
//
//    //retrofit에 client등록하기
//    fun provideRetrofitInstance(gson: Gson, client: OkHttpClient): Retrofit {
//        return Retrofit.Builder()
//            .baseUrl("http://localhost:8080")
//            .addConverterFactory(GsonConverterFactory.create(gson))
//            .client(client)
//            .build()
//    }
//
//    val tokenInterceptor : OkHttpClient = provideOkHttpClient(AccessTokenInterceptor()) //interceptor 객체 생성
//    val retrofit : Retrofit = provideRetrofitInstance(Gson(), tokenInterceptor) //retrofit 객체 생성

    override fun onStart() {
        super.onStart()

        googleLogInCheck()  // 구글 아이디로 로그인 되어 있는지 확인
    }



    /* 네이버 사용자 정보 가져오기*/
    val nidProfileCallback = object : NidProfileCallback<NidProfileResponse> {
        override fun onSuccess(response: NidProfileResponse) {
            val userId = response.profile?.id   // 고유 아이디
            val email = response.profile?.email // 이메일
            val mobile = response.profile?.mobile   // 휴대폰 번호
            val nickname = response.profile?.nickname
            val name = response.profile?.name
            val gender = response.profile?.gender
            val profileImage = response.profile?.profileImage

            val naverUserInfo = NaverToken(userId, email, name, nickname, mobile, gender, profileImage)

            tokenInFirebase.setValue("HiImYenoJe")
            // Read from the database
            tokenInFirebase.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    val value = snapshot.getValue().toString()
                    Toast.makeText(this@MainActivity, value, Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Value is: " + value)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Failed to read value.", error.toException())
                }
            })


            // retrofit 변수 생성
            val retrofit = Retrofit.Builder()
                .baseUrl("http://172.20.10.6:8080/")
                .addConverterFactory(GsonConverterFactory.create()) //kotlin to json(역 일수도)
                .build()


//            val autInfo = FirebaseDatabase.getInstance().reference
//            val aut : String = autInfo.child("User").child("Token").toString() //맞는지 모름 일단 임시
//
//            val firebase = FirebaseDatabase.getInstance().reference


            // retrofit객체 생성
            val naverLoginService = retrofit.create(NaverLoginInterface::class.java)
            val call = naverLoginService.getLoginToken(naverUserInfo)

            Log.i("call", call.toString())
            call.enqueue(object : Callback<Token> {
                override fun onResponse(call: Call<Token>, response: Response<Token>) { //요청성공시
                    if (response.isSuccessful) {
//                        Toast.makeText(this@MainActivity, "성공", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this@MainActivity, "네이버 로그인 성공\n" +
                                "user id : ${userId}\n" +
                                "email: ${email}\n" +
                                "mobile : ${mobile}\n" +
                                "nickname: ${nickname}\n" +
                                "name: ${name}\n" +
                                "gender: ${gender}\n" +
                                "profileImage: ${profileImage}\n"+
                                "accessToken: ${response.body()?.accessToken}"
                            , Toast.LENGTH_SHORT).show()
                        tokenInFirebase.setValue(first + response.body()?.accessToken) //firebase DB에 accessToken값 저장
                    } else {
                        Toast.makeText(this@MainActivity, "실패", Toast.LENGTH_SHORT).show()
                    }

                }

                override fun onFailure(call: Call<Token>, t: Throwable) { //요청실패시
                    Toast.makeText(this@MainActivity, "아예 실패", Toast.LENGTH_SHORT).show()
                    Log.e("call error", t.toString())
                }
            })
        }
        override fun onFailure(httpState : Int, message: String) {
            val errorCode = NaverIdLoginSDK.getLastErrorCode().code
            val errorDesc = NaverIdLoginSDK.getLastErrorDescription()

            Toast.makeText(this@MainActivity, "네이버 로그인 실패\n" +
                    "Error Code : ${errorCode}\n" +
                    "Error Description : ${errorDesc}", Toast.LENGTH_SHORT).show()
        }
        override fun onError(errorCode: Int, message: String) {
            onFailure(errorCode, message)
        }
    }

    /* 네이버 로그아웃 */
    private fun naverLogout() {
        NaverIdLoginSDK.logout()
        tokenInFirebase.setValue(null) //firebase DB의 accessToken에 null값 저장
        //val intent = Intent(this@MainActivity, HomeActivity::class.java) //로그인 화면으로 돌아오게 해야 함
        //startActivity(intent)
        Toast.makeText(this@MainActivity, "네이버 로그아웃 성공", Toast.LENGTH_SHORT).show()
    }

    /* 네이버 연동 해제 */
    private fun naverDeleteToken() {
        NidOAuthLogin().callDeleteTokenApi(object : OAuthLoginCallback {
            override fun onSuccess() {
                // 서버에서 토큰 삭제에 성공한 상태
                tokenInFirebase.setValue(null) //firebase DB의 accessToken에 null값 저장
                // 여기에 accessToken 담은 정보만 spring boot로 전달

                val retrofit = Retrofit.Builder()
                    .baseUrl("http://172.20.10.6:8080/")
                    .addConverterFactory(GsonConverterFactory.create()) //kotlin to json(역 일수도)
                    .build()

                val signoutService = retrofit.create(SignoutInterface::class.java)
                val call = signoutService.signout(first + tokenInFirebase.get().toString())
                Log.i("call", call.toString())
                call.enqueue(object : Callback<Token> {
                    override fun onResponse(call: Call<Token>, response: Response<Token>) { // spring boot에 데이터 전송 성공시
                        if (response.isSuccessful) {
                            googleSignInClient.revokeAccess()


                            tokenInFirebase.setValue(null) //firebase DB에 accessToken값 저장
//                    val intent = Intent(this@MainActivity, HomeActivity::class.java) //로그인 화면으로 돌아오게 해야 함
//                    startActivity(intent)

                            Toast.makeText(this@MainActivity, "성공", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Token>, t: Throwable) { //spring boot에 데이터 전송 실패시
                        Toast.makeText(this@MainActivity, "데이터 전송 실패", Toast.LENGTH_SHORT).show()
                        Log.e("call error", t.toString())
                    }
                })



                Toast.makeText(this@MainActivity, "네이버 연동 해제 성공", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(httpStatus: Int, message: String) {
                // 서버에서 토큰 삭제에 실패했어도 클라이언트에 있는 토큰은 삭제되어 로그아웃된 상태
                // 클라이언트에 토큰 정보가 없기 때문에 추가로 처리할 수 있는 작업은 없음
                tokenInFirebase.setValue(null) //firebase DB의 accessToken에 null값 저장
                Log.e(TAG, "errorCode: ${NaverIdLoginSDK.getLastErrorCode().code}")
                Log.e(TAG, "errorDesc: ${NaverIdLoginSDK.getLastErrorDescription()}")
            }
            override fun onError(errorCode: Int, message: String) {
                // 서버에서 토큰 삭제에 실패했어도 클라이언트에 있는 토큰은 삭제되어 로그아웃된 상태
                // 클라이언트에 토큰 정보가 없기 때문에 추가로 처리할 수 있는 작업은 없음
                onFailure(errorCode, message)
            }
        })
    }

    /* Google Client 초기화 */
    private fun getGoogleClient() : GoogleSignInClient {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //.requestIdToken(getString(R.string.google_client_id))   // Id Token 값 요청
            .requestEmail() // Email 요청
            .requestProfile()   // 프로필 정보 요청
            .build()

        Log.i("Request to Google", "Send Success")

        return GoogleSignIn.getClient(this@MainActivity, googleSignInOptions)
    }

    /* Google 로그인 초기화 */
    private fun googleAuthLauncher() {
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task : Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)

                handleSignInResult(task)    // Google 사용자 정보 받아오기
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                // 사용자가 로그인을 취소한 경우
                Log.e(TAG, "Google Sign-In canceled by user")
                Toast.makeText(this@MainActivity, "Google Sign-In canceled by user", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "Google Sign-In failed with resultCode: ${result.resultCode}")
                Toast.makeText(this@MainActivity, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* Google 사용자 정보 받아오기 */
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            val userId = account?.id // 식별 ID 값
            val email = account?.email // Email
            val name = account?.displayName // 이름
//            val token = account?.idToken // 토큰값

            val googleUserInfo = GoogleToken(userId, email, name) //dto와 다름

//            Toast.makeText(this, "구글 로그인 성공\n" +
//                    "User Id : ${userId}\n" +
//                    "Email : ${email}\n" +
//                    "Full Name : ${name}\n" +
//                    "Token : ${token}", Toast.LENGTH_SHORT).show()

            Toast.makeText(this, "구글 로그인 성공\n" +
                    "User Id : ${userId}\n" +
                    "Email : ${email}\n" +
                    "Full Name : ${name}\n", Toast.LENGTH_SHORT).show()

            val retrofit = Retrofit.Builder()
                .baseUrl("http://172.20.10.6:8080/")
                .addConverterFactory(GsonConverterFactory.create()) //kotlin to json(역 일수도)
                .build()

            val googleLoginService = retrofit.create(GoogleLoginInterface::class.java)
            val call = googleLoginService.getLoginToken(googleUserInfo)
            Log.i("call", call.toString())
            call.enqueue(object : Callback<Token> {
                override fun onResponse(call: Call<Token>, response: Response<Token>) { //요청성공시
                    if (response.isSuccessful) {
//                        Toast.makeText(this@MainActivity, "성공", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this@MainActivity, "네이버 로그인 성공\n" +
                                "user id : ${userId}\n" +
                                "email: ${email}\n" +
                                "name: ${name}\n" +
                                "accessToken: ${response.body()?.accessToken}"
                            , Toast.LENGTH_SHORT).show()
                        tokenInFirebase.setValue(first + response.body()?.accessToken) //firebase DB에 accessToken값 저장
                    } else {
                        Toast.makeText(this@MainActivity, "실패", Toast.LENGTH_SHORT).show()
                    }

                }

                override fun onFailure(call: Call<Token>, t: Throwable) { //요청실패시
                    Toast.makeText(this@MainActivity, "아예 실패", Toast.LENGTH_SHORT).show()
                    Log.e("call error", t.toString())
                }
            })

            Log.i("LogIn", "구글 로그인 성공")
//            Log.i("Token", token.toString())
        } catch (e: ApiException) {
            Toast.makeText(this@MainActivity, "구글 로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("GoogleSignIn", "구글 로그인 실패: ${e.message}")
        }
    }


    /* Google 로그아웃 */
    private fun googleLogout() { //로그아웃.setOnClickListener 눌리면 로그아웃되게 하는 메소드? 로그인화면으로 이동시켜야겠네
        googleSignInClient.signOut() //이게 뭐징?
        tokenInFirebase.setValue(null) //firebase DB의 accessToken에 null값 저장
        //val intent = Intent(this@MainActivity, HomeActivity::class.java) //로그인 화면으로 돌아오게 해야 함
        //startActivity(intent)
        Toast.makeText(this@MainActivity, "구글 로그아웃 성공", Toast.LENGTH_SHORT).show()
    }

    /* Google 연동 해제 */
    private fun googleDeleteToken() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://172.20.10.6:8080/")
            .addConverterFactory(GsonConverterFactory.create()) //kotlin to json(역 일수도)
            .build()

        val signoutService = retrofit.create(SignoutInterface::class.java)
        val call = signoutService.signout(first + tokenInFirebase.get().toString())
        Log.i("call", call.toString())
        call.enqueue(object : Callback<Token> {
            override fun onResponse(call: Call<Token>, response: Response<Token>) { // spring boot에 데이터 전송 성공시
                if (response.isSuccessful) {
                    googleSignInClient.revokeAccess()


                    tokenInFirebase.setValue(null) //firebase DB에 accessToken값 저장
//                    val intent = Intent(this@MainActivity, HomeActivity::class.java) //로그인 화면으로 돌아오게 해야 함
//                    startActivity(intent)

                    Toast.makeText(this@MainActivity, "성공", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Token>, t: Throwable) { //spring boot에 데이터 전송 실패시
                Toast.makeText(this@MainActivity, "데이터 전송 실패", Toast.LENGTH_SHORT).show()
                Log.e("call error", t.toString())
            }
        })
    }

    /* 구글 아이디로 로그인 되어 있는지 확인 */
    private fun googleLogInCheck() {
        val account = GoogleSignIn.getLastSignedInAccount(this) // 로그인 되어 있는지 가져옴
        if (account != null) {
            Toast.makeText(this@MainActivity, "이미 로그인 됨", Toast.LENGTH_SHORT).show()

            resultLauncher.launch(googleSignInClient.signInIntent)  // 구글 로그인 창으로 넘어감
        }
    }

    private fun isAccessTokenAlive(){ //만료일 지났을 경우 현재 토큰을 갱신 토큰으로 변경
//        val refreshToken = NaverIdLoginSDK.getRefreshToken().toString() // 갱신 토큰
//        val expiresAt = NaverIdLoginSDK.getExpiresAt().toString()


//        if(LocalDateTime.now().toString() > NaverIdLoginSDK.getExpiresAt().toString()){
//
//        }

    }

}


// 이연제 해야 할 일
// 네이버 로그인 성공하면 accessToken firebase에 저장. (완료)
// 구글 data class 등 추가해서 구글 로그인도 마무리. (완료)
// 네이버, 구글 로그아웃, 회원탈퇴 구현. (완료)