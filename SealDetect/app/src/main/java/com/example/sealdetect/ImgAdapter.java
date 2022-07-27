package com.example.sealdetect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.decoration.HorizontalItemDecoration;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.style.PictureSelectorStyle;

import java.lang.annotation.Target;
import java.util.ArrayList;

public class ImgAdapter extends RecyclerView.Adapter<ImgAdapter.RecyclerViewHolder>{
    protected ArrayList<Img> imgs;
    protected ArrayList<LocalMedia>medias;
    protected LayoutInflater mInflater;
    protected Context context;
    public ImgAdapter(Context context, ArrayList<Img> imgs) {
        mInflater = LayoutInflater.from(context);
        this.imgs = imgs;
        this.context = context;
        this.medias = new ArrayList<LocalMedia>();
        for(Img img:imgs){
            String load_url = Const.baseurl+"/download/"+img.name;
            LocalMedia new_img = LocalMedia.generateLocalMedia(load_url, PictureMimeType.ofPNG());
            this.medias.add(new_img);
        }
        System.out.println(this.medias.size());
    }

    @Override
    public int getItemViewType(final int position) {
        return R.layout.img_item;
    }

    @NonNull
    @Override
    public ImgAdapter.RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new ImgAdapter.RecyclerViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ImgAdapter.RecyclerViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if(imgs.size()>0){
            Img img = imgs.get(position);
            holder.item_filename.setText(img.name);
            if(img.hasseal){
                holder.item_hasseal.setText("有印章");
            }
            else{
                holder.item_hasseal.setText("无印章");
            }
            String load_url = Const.baseurl+"/download/"+img.name;
            Glide.with(this.context.getApplicationContext())
                    .applyDefaultRequestOptions(
                            RequestOptions
                                    .skipMemoryCacheOf(true)
                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                    .load(load_url.trim())
                    .timeout(6000) // 60 second timeout
                    .into(holder.item_img);
            holder.item_img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PictureSelector.create(context)
                            //.openGallery(SelectMimeType.ofAudio())
                            .openPreview()
                            .setImageEngine(GlideEngine.createGlideEngine())
                            .isPreviewFullScreenMode(true)
                            .setSelectorUIStyle(new PictureSelectorStyle())
                            .startActivityPreview(position, false, medias);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return imgs.size();
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder{
        private ImageView item_img;
        private TextView item_filename,item_hasseal;
        //private ImgAdapter adapter;
        public RecyclerViewHolder(@NonNull View itemView,ImgAdapter adapter) {
            super(itemView);
            //this.adapter = adapter;
            this.item_hasseal = itemView.findViewById(R.id.item_hasseal);
            this.item_img = itemView.findViewById(R.id.item_img);
            this.item_filename = itemView.findViewById(R.id.item_filename);
        }
    }
}
