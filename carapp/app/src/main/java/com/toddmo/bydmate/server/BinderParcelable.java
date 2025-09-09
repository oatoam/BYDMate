package com.toddmo.bydmate.server;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class BinderParcelable implements Parcelable {
    public static final Parcelable.Creator<BinderParcelable> CREATOR = new Creator<BinderParcelable>() {
        @Override
        public BinderParcelable createFromParcel(Parcel source) {
            return new BinderParcelable(source);
        }

        @Override
        public BinderParcelable[] newArray(int size) {
            return new BinderParcelable[0];
        }
    };

    public IBinder mBinder;

    public BinderParcelable(IBinder binder) {
        this.mBinder = binder;
    }

    public BinderParcelable(Parcel source) {
        this.mBinder = source.readStrongBinder();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStrongBinder(mBinder);
    }
}