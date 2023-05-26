package com.PdfCreator.Editor.adapter

import PdfCreator.Editor.R
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.PdfCreator.Editor.history.HistoryModel
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs


//class ContactRVAdapter(variable for context: Context, variable for on Item click interface )
class HistoryAdapter(
    val context: Context,
    val pdfClickInterface: PdfClickInterface,
    val pdfLongClickInterface: PdfLongClickInterface,
    val pdfList: ArrayList<HistoryModel> = ArrayList<HistoryModel>()
) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    // private val pdfList = ArrayList<HistoryModel>()
    private var mAreCheckboxesVisible = false
    val currentSelectedItems: List<HistoryModel> = ArrayList<HistoryModel>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pdfName: TextView = itemView.findViewById(R.id.docName)
        val pdfTimeStamp: TextView = itemView.findViewById(R.id.pdfcreatedDate)
        val checkBoxSelect: CheckBox = itemView.findViewById(R.id.selectPdf)
        // val menuItem: ImageView = itemView.findViewById(R.id.toolbarIconMenu)


    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //we will be inflating our layout here
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_pdf_history,
            parent,
            false
        )
        return ViewHolder(itemView)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        var data = pdfList.get(position)
        holder.pdfName.setText(data.fileName)
        holder.pdfTimeStamp.setText(data.pdfCreatedDate)

        holder.itemView.setOnClickListener {
            multipleControl(holder.itemView)
            pdfClickInterface.onPdfClick(data)
        }


        holder.checkBoxSelect.setVisibility(if(mAreCheckboxesVisible)
            View.VISIBLE
        else
            View.GONE)

        holder.checkBoxSelect.setOnCheckedChangeListener(null)
        holder.checkBoxSelect.isChecked = data.selected

        holder.itemView.setOnLongClickListener(OnLongClickListener {
            mAreCheckboxesVisible = true

            notifyDataSetChanged()
            pdfLongClickInterface.onPdfLongClick(data, position)

            // pdfLongClickInterface.onPdfLongClick(data)
            true
        })


        holder.checkBoxSelect.setOnCheckedChangeListener { buttonView, isChecked ->
            /*if (holder.checkBoxSelect.isChecked) {*/
            // position will give you the position of the clicked element from where you can fetch your data
            //   val pos = holder.checkBoxSelect.getTag() as Int
            var selectedItem = pdfList[position]
            selectedItem.selected = isChecked
            data = pdfList[position]
            Log.e("TAG", "onBindViewHolder:$pdfList ")
            Log.e("TAG1", "onBindViewHolder:$data ")

            val gson = Gson()
            val jsonString = gson.toJson(pdfList)
            // val json = gson.toJson(jsonString)
            Log.e("TAG", "onBindViewHolder: $jsonString")
            Prefs.putString("checkedList", jsonString)
        }


    }

    private fun multipleControl(itemView: View) {
        itemView.setEnabled(false)
        Handler().postDelayed(Runnable { // This method will be executed once the timer is over
           itemView.setEnabled(true)
        }, 1000) // set time as per your requirement



    }

    private fun saveCheckedItems() {

    }

    override fun getItemCount(): Int {
        return pdfList.size
    }

    fun updateList(newList: List<HistoryModel>) {
        pdfList.clear()
        pdfList.addAll(newList)
        notifyDataSetChanged()
    }


}


interface PdfClickInterface {
    fun onPdfClick(historyModel: HistoryModel)
}

interface PdfLongClickInterface {
    fun onPdfLongClick(historyModel: HistoryModel, position: Int)
}


