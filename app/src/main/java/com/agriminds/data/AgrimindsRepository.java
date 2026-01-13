package com.agriminds.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.agriminds.data.dao.ListingDao;
import com.agriminds.data.dao.UserDao;
import com.agriminds.data.entity.Listing;
import com.agriminds.data.entity.User;

import java.util.List;

public class AgrimindsRepository {

    private UserDao userDao;
    private ListingDao listingDao;
    private LiveData<List<Listing>> allListings;

    public AgrimindsRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        userDao = db.userDao();
        listingDao = db.listingDao();
        allListings = listingDao.getAllActiveListings();
    }

    public AgrimindsRepository(AppDatabase db) {
        userDao = db.userDao();
        listingDao = db.listingDao();
        allListings = listingDao.getAllActiveListings();
    }

    public LiveData<List<Listing>> getAllListings() {
        return allListings;
    }

    public long insertUser(User user) {
        return userDao.insert(user);
    }

    public User getUserByEmailAndPassword(String email, String password) {
        return userDao.login(email, password);
    }

    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    public User getUserByPhone(String phone) {
        return userDao.getUserByPhone(phone);
    }

    public User getUserByName(String fullName) {
        return userDao.getUserByName(fullName);
    }

    public User getUserById(int userId) {
        return userDao.getUserById(userId);
    }

    public void insertListing(Listing listing) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            listingDao.insert(listing);
        });
    }
}
