package com.example.ricarica
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database


@Composable
fun FBView (){

     var counter = remember { mutableStateOf(0) }
     var name = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        Firebase.database
            .getReference("data").child("incoming").addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        name.value = snapshot.value.toString()
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                }
            )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(name.value, fontSize = 32.sp)
        Button(onClick = {
            Firebase.database
                .getReference("data")
                .push()
                .setValue(counter.value++)
        //creo una nuova chiave univoca e ogni volta che apro ilbottone succede qualcosa
        }) {
            Text(text = "Add")
        }



    }

}