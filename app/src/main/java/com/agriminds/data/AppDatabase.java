package com.agriminds.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.agriminds.data.dao.AnswerViewDao;
import com.agriminds.data.dao.CropDiseaseDao;
import com.agriminds.data.dao.ExpertAnswerDao;
import com.agriminds.data.dao.ExpertRatingDao;
import com.agriminds.data.dao.HiddenQuestionDao;
import com.agriminds.data.dao.ListingDao;
import com.agriminds.data.dao.MarketPriceDao;
import com.agriminds.data.dao.QuestionDao;
import com.agriminds.data.dao.ReplyDao;
import com.agriminds.data.dao.UserDao;
import com.agriminds.data.entity.AnswerView;
import com.agriminds.data.entity.CropDisease;
import com.agriminds.data.entity.ExpertAnswer;
import com.agriminds.data.entity.ExpertRating;
import com.agriminds.data.entity.HiddenQuestion;
import com.agriminds.data.entity.Listing;
import com.agriminds.data.entity.MarketPrice;
import com.agriminds.data.entity.Question;
import com.agriminds.data.entity.Reply;
import com.agriminds.data.entity.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = { User.class, MarketPrice.class, Listing.class,
        CropDisease.class, Question.class, ExpertAnswer.class,
        ExpertRating.class, AnswerView.class, Reply.class, HiddenQuestion.class }, version = 9, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();

    public abstract MarketPriceDao marketPriceDao();

    public abstract ListingDao listingDao();

    public abstract CropDiseaseDao cropDiseaseDao();

    public abstract QuestionDao questionDao();

    public abstract ExpertAnswerDao expertAnswerDao();

    public abstract ExpertRatingDao expertRatingDao();

    public abstract AnswerViewDao answerViewDao();

    public abstract ReplyDao replyDao();

    public abstract HiddenQuestionDao hiddenQuestionDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Migration from version 6 to 7: Add hidden_questions table
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the new hidden_questions table
            database.execSQL("CREATE TABLE IF NOT EXISTS `hidden_questions` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`expertId` INTEGER NOT NULL, " +
                    "`questionId` INTEGER NOT NULL, " +
                    "`hiddenAt` INTEGER NOT NULL)");
        }
    };

    // Migration from version 7 to 8: Add audioPath to expert_answers
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE expert_answers ADD COLUMN audioPath TEXT");
        }
    };

    // Migration from version 8 to 9: Add audioPath to questions
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE questions ADD COLUMN audioPath TEXT");
        }
    };

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Populate database with test users
            databaseWriteExecutor.execute(() -> {
                createTestUsers();
            });
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // Ensure test users exist every time database opens
            databaseWriteExecutor.execute(() -> {
                createTestUsers();
            });
        }
    };

    private static void createTestUsers() {
        UserDao userDao = INSTANCE.userDao();

        // Check if test farmer exists, create if not
        User existingFarmer = userDao.getUserByEmail("farmer@test.com");
        if (existingFarmer == null) {
            User farmer = new User();
            farmer.fullName = "Test Farmer";
            farmer.email = "farmer@test.com";
            farmer.password = "123456";
            farmer.phone = "01712345678";
            farmer.userType = "FARMER";
            farmer.district = "Dhaka";
            farmer.upazila = "Savar";
            farmer.farmSize = "5 acres";
            farmer.farmingType = "Rice & Vegetables";
            userDao.insert(farmer);
            android.util.Log.d("AppDatabase", "Test farmer created: farmer@test.com");
        }

        // Check if test expert exists, create if not
        User existingExpert = userDao.getUserByEmail("expert@test.com");
        if (existingExpert == null) {
            User expert = new User();
            expert.fullName = "Dr. Expert";
            expert.email = "expert@test.com";
            expert.password = "123456";
            expert.phone = "01798765432";
            expert.userType = "EXPERT";
            expert.specialization = "Crop Diseases";
            expert.experience = "10 years";
            expert.organization = "Agricultural Institute";
            userDao.insert(expert);
            android.util.Log.d("AppDatabase", "Test expert created: expert@test.com");
        }

        android.util.Log.d("AppDatabase",
                "Test users available - farmer@test.com and expert@test.com (password: 123456)");
    }

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "agriminds_database")
                            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .allowMainThreadQueries()
                            .build();

                    // Ensure test users exist immediately
                    databaseWriteExecutor.execute(() -> {
                        try {
                            Thread.sleep(100); // Wait for database to initialize
                            createTestUsers();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
        return INSTANCE;
    }

    public static AppDatabase getInstance(Context context) {
        return getDatabase(context);
    }
}
