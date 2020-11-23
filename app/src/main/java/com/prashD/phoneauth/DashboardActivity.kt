package com.prashD.phoneauth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.prashD.phoneauth.databinding.ActivityDashboardBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {
    private val personCollectionRef = Firebase.firestore.collection("person")
    lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityDashboardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        if(auth.currentUser==null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
        binding.btnLogout.setOnClickListener{
            auth.signOut()
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }

        binding.btnSave.setOnClickListener {
            val person = Person("harry",19,"Hogwarts")
            saveData(person)
        }
        binding.btnShowData.setOnClickListener {
            getData()
        }
        binding.btnRangeTest.setOnClickListener {
            retrieveDataInRange()
        }
        binding.btnDelete.setOnClickListener {
            val person = Person("harry",18,"Hogwarts")
            deletePerson(person)
        }
        binding.btnBatchedWrites.setOnClickListener {
            changeNameAndLocation("SBumPaWdyMh9XiizoLPM","Mary","Hognath")
        }
        binding.btnTransaction.setOnClickListener {
            birthday("SBumPaWdyMh9XiizoLPM")
        }
        showDataInRealtimeUpdates()
    }


    private fun retrieveDataInRange() = CoroutineScope(Dispatchers.IO).launch{
        try {
            val querySnapshot = personCollectionRef
                .whereGreaterThan("age",18)
                .whereLessThan("age",20)
                .orderBy("age")
                .get().await()
            val sb = StringBuilder()
            for(document in querySnapshot.documents){
                sb.append("${document.toObject(Person::class.java)}\n")
            }
            withContext(Dispatchers.Main){
                binding.tvContent.text = sb.toString()
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, "Data retrieved successfully", Toast.LENGTH_LONG).show()
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDataInRealtimeUpdates(){
        personCollectionRef.addSnapshotListener{querySnapshot,firebaseFirestoreException->
            firebaseFirestoreException?.let {
                Toast.makeText(this@DashboardActivity, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            querySnapshot?.let{
                val sb = StringBuilder()
                for(document in querySnapshot.documents){
                    sb.append("${document.toObject(Person::class.java)}\n")
                }
                binding.tvContent.text = sb.toString()
            }
        }
    }

    private fun getData() = CoroutineScope(Dispatchers.IO).launch{
        try {
            val querySnapshot = personCollectionRef.get().await()
            val sb = StringBuilder()
            for(document in querySnapshot.documents){
                sb.append("${document.toObject(Person::class.java)}\n")
            }
            withContext(Dispatchers.Main){
                binding.tvContent.text = sb.toString()
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, "Data retrieved successfully", Toast.LENGTH_LONG).show()
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveData(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, "Data saved successfully", Toast.LENGTH_LONG).show()
            }
        }catch (e:Exception){
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updatePerson(person: Person, newPersonMap: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("name", person.name)
            .whereEqualTo("location", person.location)
            .whereEqualTo("age", person.age)
            .get()
            .await()
        if(personQuery.documents.isNotEmpty()) {
            for(document in personQuery) {
                try {
                    //personCollectionRef.document(document.id).update("age", newAge).await()
                    personCollectionRef.document(document.id).set(
                        newPersonMap,
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, "No persons matched the query.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deletePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("name", person.name)
            .whereEqualTo("location", person.location)
            .whereEqualTo("age", person.age)
            .get()
            .await()
        if(personQuery.documents.isNotEmpty()) {
            for(document in personQuery) {
                try {
                    // To delete all the entries of a person
                    personCollectionRef.document(document.id).delete().await()

                    // To delete only some fields of a person
//                    personCollectionRef.document(document.id).update(mapOf(
//                        "location" to FieldValue.delete()
//                    ))
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, "No persons matched the query.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Batched Writes- Apply changes when none of the changes fail
    private fun changeNameAndLocation(personId:String, newName:String, newLocation:String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runBatch { batch->
                val personRef = personCollectionRef.document(personId)
                batch.update(personRef,"name",newName)
                batch.update(personRef,"location",newLocation)
            }.await()
        }catch (e: Exception){
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun birthday(personId: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runTransaction {
                val personRef = personCollectionRef.document(personId)
                val person = it.get(personRef)
                val newAge = person["age"] as Long + 1
                it.update(personRef,"age",newAge)
                null
            }.await()

        }catch (e: Exception){
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}