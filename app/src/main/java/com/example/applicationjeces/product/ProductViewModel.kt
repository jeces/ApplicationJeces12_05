package com.example.applicationjeces.product

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.applicationjeces.chat.ChatData
import com.example.applicationjeces.chat.ChatroomData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/* 뷰모델은 DB에 직접 접근하지 않아야함. Repository 에서 데이터 통신 */
class ProductViewModel(application: Application): AndroidViewModel(application) {

    /* 각종 라이브데이터 DocumentSnapshot은 firestore와 연결되어있어서 firestore가 변경되면 변경됨 하지만 다른것들은 바꿔줘야함. Snapshot으로 최대한 뽑아보자 */
    var liveTodoData = MutableLiveData<List<DocumentSnapshot>>()
    var productArrayList: MutableList<Product> = ArrayList()
    var chatArrayList: MutableList<ChatroomData> = ArrayList()
    var liveTodoChatData = MutableLiveData<List<DocumentSnapshot>>()
    var liveTodoChatroomData = MutableLiveData<List<DocumentSnapshot>?>()

    /* 채팅 담을 리스트 */
    val listChat : MutableList<ChatData> = mutableListOf()
    /* 채팅 실시간 라이브 데이터 */
    val liveTodoChatDataList = MutableLiveData<List<ChatData>?>()

    var jecesfirestore: FirebaseFirestore? = null
    var thisUser: String? = null
    var position: Int = 0

    var documentId : String? = null

    var imgList: ArrayList<String> = arrayListOf()

    init {
        /* firebase 연동 */
        jecesfirestore = FirebaseFirestore.getInstance()

        /* 현재 로그인 아이디 */
        thisUser = FirebaseAuth.getInstance().currentUser?.email.toString()

        /* firebase product 전체 가져오기 */
        /* https://velog.io/@nagosooo/%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9C-TodoList%EC%95%B1-%EB%A7%8C%EB%93%A4%EA%B8%B0 */
        allProduct()
        allChatroom()
    }

    /* firebase storage에서 이미지 가져오기 */
    fun getImage(productName:String, productCount: Int): MutableList<String>? {
        imgList.clear()
        /* 글자 나누기 */
        /* 카운트는 가져와야함 product에 저장해놓고 */
        /* User이름, 상품이름, 사진갯수몇가지인지[product에 추가할것], 사진idx값 가져오기 */
        return if(productCount <= 0) {
            var word = "basic_img.png"
            imgList.add(word)
            imgList
        } else {
            for(i: Int in 0 until productCount) {
                /* 워드를 가져와서 돌림 */
                var word: String = thisUser + "_" + productName + "_" + i + "_IMAGE_.png"
                imgList.add(word)
            }
            imgList
        }
    }

    /* 자신의 채팅목록 전체 가져오기 */
    fun allChatroom() {
        /* 어떻게 가져올껀지 찾아야한다. */
        jecesfirestore!!.collection("/Chatroom").orderBy("time", Query.Direction.DESCENDING).addSnapshotListener { chatrooms, e->
            if (e != null) {
                return@addSnapshotListener
            }
            val response = Response()
            for(snapshot in chatrooms!!.documents) {
                Log.d("아이디", thisUser.toString())
                if(snapshot.getString("id")!!.contains(thisUser.toString())) {
                    snapshot?.let {
                        if(response.products == null) {
                            response.products = listOf(it)

                        } else {
                            response.products = response.products?.plus(listOf(it))
                        }
                    }
                }
            }
            liveTodoChatroomData.value = response.products
        }

    }

    /* firebase Product 전체 가져오기 */
    fun allProduct() {
        jecesfirestore!!.collection("Product").addSnapshotListener { products, e ->
            if (e != null) {
                return@addSnapshotListener
            }
            liveTodoData.value = products?.documents
        }
    }

    /* Chat comment 생성 */
    fun addChat(chat: ChatData) {

        jecesfirestore!!.collection("Chat").add(chat)
            .addOnSuccessListener {
                // 성공할 경우
                Log.d("데이터순서", "인서트")
                Log.w("CHAT 데이터 입력 성공", "Error getting documents")
                listChat.add(chat)
                liveTodoChatDataList.value = listChat
                documentId = it.id
            }.addOnFailureListener { exception ->
                // 실패할 경우
                Log.w("CHAT 데이터 입력 실패", "Error getting documents")
            }

        /* 채팅방 수정 */
        val dbRef = jecesfirestore!!.collection("Chatroom")
        dbRef.whereEqualTo("chatidx", chat.chatroomidx).get().addOnCompleteListener {
            if(it.isSuccessful) {
                for(document in it.result) {
                    val update: MutableMap<String, Any> = HashMap()
                    update["lastcomment"] = chat.content
                    update["time"] = chat.time
                    dbRef.document(document.id).set(update, SetOptions.merge())
                }
            }
        }
    }
    /* 제일 마지막 데이터 가져오기 */
    fun lastChat(chat : ChatData) {
        val dbRef = jecesfirestore!!.collection("Chat")
        Log.d("라스트데이터", "123")
        if(changeTime(listChat.last().time) == changeTime(chat.time) && listChat.last().myid == chat.myid && listChat.last().chatroomidx == chat.chatroomidx && listChat.isNotEmpty()) {
            Log.d("라스트데이터", "1234")
            dbRef.whereEqualTo("chatroomidx", chat.chatroomidx).orderBy("time", Query.Direction.DESCENDING).limit(2).get().addOnCompleteListener {
                if(it.isSuccessful) {
                    for(document in it.result) {
                        Log.d("데이터순서", "${document.id.equals(documentId)}")
                        Log.d("데이터순서", "${listChat.last().time} / ${document.getTimestamp("time").toString()}")
                        if((document.getString("myid").toString() == chat.myid) && (document.id == documentId)) {
                            Log.d("라스트데이터", document.toString())
                            val update: MutableMap<String, Any> = HashMap()
                            update["fronttimesame"] = "true"
                            dbRef.document(document.id).set(update, SetOptions.merge())
                        }
                    }
                }
                /* 여기서 데이터 넣어서 업데이트 해주자
                *  위쪽 리스트 다시보고 수정할 것 테스트 필요함
                * */
                addChat(chat)
            }
        } else {

        }
    }

    /* 시간변환 */
    fun changeTime(timestamp: Timestamp): String {
        val mils = timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000
//        val sf = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss", Locale.KOREA)
        val sf = SimpleDateFormat("aa hh:mm", Locale.KOREA)
        val nDate = Date(mils)
        val date = sf.format(nDate).toString()
        return date
    }



    /* Chat 가져오기 */
    fun getChat2(idx: String) {
        /* 데이터베이스 담기 */
        /* 이것도 response를 만들어서 해줘야하는 듯 */
        jecesfirestore!!.collection("Chat").whereEqualTo("chatroomidx", idx).orderBy("time", Query.Direction.ASCENDING).addSnapshotListener { chat, e ->
            if(e != null) {
                return@addSnapshotListener
            }
            liveTodoChatData.value = chat?.documents
        }
    }

    /* Chat 가져오기 */
    fun getChat(idx: String) {
        /* 데이터베이스 담기 */
        /* 이것도 response를 만들어서 해줘야하는 듯 */
        listChat.clear()
        jecesfirestore!!.collection("Chat").whereEqualTo("chatroomidx", idx).orderBy("time", Query.Direction.ASCENDING).get().addOnCompleteListener { chat ->
            if(chat.isSuccessful) {
                for(document in chat.result) {
                    val chatDatas = ChatData(
                        document.getString("chatroomidx").toString(),
                        document.getString("content").toString(),
                        document.getString("myid").toString(),
                        document.getTimestamp("time") as Timestamp,
                        document.getString("fronttimesame").toString()
                    )
                    listChat?.add(chatDatas)
                }
            }
            liveTodoChatDataList.value = listChat
        }
    }

    /* firebase Product 입력 */
    fun addProducts(product: Product) {
        val products = hashMapOf(
            "ID" to thisUser,
            "productName" to product.product_name,
            "productPrice" to product.product_price,
            "productDescription" to product.product_description,
            "productCount" to product.product_count,
            "productImgUrl" to product.product_img_url
        )
        jecesfirestore!!.collection("Product").add(products)
            .addOnSuccessListener {
                // 성공할 경우
                Log.w("PRODUCT 데이터 입력 성공", "Error getting documents")
            }
            .addOnFailureListener { exception ->
                // 실패할 경우
                Log.w("PRODUCT 데이터 입력 실패", "Error getting documents")
            }
    }

    /* firebase 검색 */
    /* firestore에서는 like를 사용못함 */
    /* 비동기 앱의 문제점 */
    /* observer 기능을 사용하기 위해 데이터 하나를 수정해줌 */
    /* firesotre가 제일 늦게 반응해 그다음 검색 때 바뀜 */
    /* 서치뷰에 suspend를 쓸수가없음 override 고정되어있어서 await 못씀 */
    /* 아래 서치뷰 검색을 씀 */
    /* 서치뷰 검색어로 검색 */
    fun searchProductsCall(searchName: String)  : MutableLiveData<Response> {
        val searchLiveTodoData = MutableLiveData<Response>()
        jecesfirestore!!.collection("Product").get().addOnCompleteListener  { productSearch ->
            val response = Response()
            for (snapshot in productSearch.result) {
                /* 검색했을 때 있다면 리스트 넣기 */
                if (snapshot.getString("productName")!!.contains(searchName)) {
                    snapshot?.let {
                        if(response.products == null) {
                            response.products = listOf(it)
                        } else {
                            response.products = response.products?.plus(listOf(it))
                        }
                    }
                }
            }
            searchLiveTodoData.value = response
        }
        return searchLiveTodoData
    }

    /* 채팅 디테일 데이터를 가지고 있는 데이터 */
    fun setChatDetail(chatidx: String, lastcomment: String, myid: String, yourid: String, getPosition: Int) {
        chatArrayList.clear()
        val chatDetail = ChatroomData(chatidx, lastcomment, myid, yourid)
        position = getPosition
        chatArrayList.add(chatDetail)
    }

    /* 제품 디테일 데이터를 가지고 있는 데이터 */
    fun setProductDetail(productName: String, productPrice: String, productDescription: String, productCount: String, getPosition: Int) {
        productArrayList.clear()
        val productDetail = Product(0, productName, productPrice, productDescription, productCount.toInt(), thisUser + "_" + productName + "_0_IMAGE_.png")
        position = getPosition
        productArrayList.add(productDetail)
    }

    /* 상대방 이름 가져오기 */
    fun getYourId(idx: String) {
        /* 데이터베이스 담기 */
        /* 이것도 response를 만들어서 해줘야하는 듯 */
        jecesfirestore!!.collection("Chatroom").whereEqualTo("chatroomidx", idx).addSnapshotListener { chat, e ->
            if(e != null) {
                return@addSnapshotListener
            }
            liveTodoChatroomData.value = chat?.documents
        }
    }
}




