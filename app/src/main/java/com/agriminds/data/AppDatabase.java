package com.agriminds.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.agriminds.data.dao.AnswerViewDao;
import com.agriminds.data.dao.CropChartCommentDao;
import com.agriminds.data.dao.CropChartDao;
import com.agriminds.data.dao.CropDiseaseDao;
import com.agriminds.data.dao.ExpertAnswerDao;
import com.agriminds.data.dao.ExpertRatingDao;
import com.agriminds.data.dao.HiddenQuestionDao;
import com.agriminds.data.dao.ListingDao;
import com.agriminds.data.dao.QuestionDao;
import com.agriminds.data.dao.ReplyDao;
import com.agriminds.data.dao.UserDao;
import com.agriminds.data.dao.CropChartStarDao;
import com.agriminds.data.dao.CropChartReplyDao;
import com.agriminds.data.entity.AnswerView;
import com.agriminds.data.entity.CropChart;
import com.agriminds.data.entity.CropChartComment;
import com.agriminds.data.entity.CropChartStar;
import com.agriminds.data.entity.CropChartReply;
import com.agriminds.data.entity.CropDisease;
import com.agriminds.data.entity.ExpertAnswer;
import com.agriminds.data.entity.ExpertRating;
import com.agriminds.data.entity.HiddenQuestion;
import com.agriminds.data.entity.Listing;
import com.agriminds.data.entity.Question;
import com.agriminds.data.entity.Reply;
import com.agriminds.data.entity.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = { User.class, Listing.class, CropDisease.class, Question.class, ExpertAnswer.class,
        ExpertRating.class, AnswerView.class, Reply.class, HiddenQuestion.class, CropChart.class,
        CropChartComment.class, CropChartStar.class, CropChartReply.class,
        com.agriminds.data.entity.ToDo.class }, version = 18, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Migration from version 17 to 18: Add todos table
    static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `todos` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`userId` TEXT, "
                    + "`task` TEXT, "
                    + "`isCompleted` INTEGER NOT NULL, "
                    + "`createdAt` INTEGER NOT NULL)");
        }
    };

    // Migration from version 16 to 17: Add hasEverBeenShared to crop_charts
    static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE crop_charts ADD COLUMN hasEverBeenShared INTEGER NOT NULL DEFAULT 0");

            // Initialize: If currently shared, it has obviously been shared
            database.execSQL("UPDATE crop_charts SET hasEverBeenShared = 1 WHERE isShared = 1");
        }
    };

    // Migration from version 15 to 16: Add persistent shared charts stat
    static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE users ADD COLUMN totalChartsShared INTEGER NOT NULL DEFAULT 0");

            // Initialize with current count of shared charts
            database.execSQL("UPDATE users SET totalChartsShared = " +
                    "(SELECT COUNT(*) FROM crop_charts WHERE crop_charts.farmerId = users.id AND crop_charts.isShared = 1)");
        }
    };

    // Migration from version 14 to 15: Add persistent user stats
    static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add new columns
            database.execSQL("ALTER TABLE users ADD COLUMN totalQuestionsAsked INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE users ADD COLUMN totalAnswersReceived INTEGER NOT NULL DEFAULT 0");

            // Initialize with current counts (Best effort for existing data)
            // Initialize totalQuestionsAsked
            database.execSQL("UPDATE users SET totalQuestionsAsked = " +
                    "(SELECT COUNT(*) FROM questions WHERE questions.farmerId = users.id)");

            // Initialize totalAnswersReceived (Count questions that have answers)
            database.execSQL("UPDATE users SET totalAnswersReceived = " +
                    "(SELECT COUNT(*) FROM questions WHERE questions.farmerId = users.id AND (questions.answerCount > 0 OR questions.status = 'Answered'))");
        }
    };

    static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `crop_chart_replies` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`commentId` INTEGER NOT NULL, " +
                    "`userId` TEXT, " +
                    "`userName` TEXT, " +
                    "`userType` TEXT, " +
                    "`reply` TEXT, " +
                    "`timestamp` INTEGER NOT NULL)");
        }
    };

    public abstract com.agriminds.data.dao.ToDoDao toDoDao();

    public abstract CropChartStarDao cropChartStarDao();

    public abstract CropChartReplyDao cropChartReplyDao();

    public abstract UserDao userDao();

    public abstract ListingDao listingDao();

    public abstract CropDiseaseDao cropDiseaseDao();

    public abstract QuestionDao questionDao();

    public abstract ExpertAnswerDao expertAnswerDao();

    public abstract ExpertRatingDao expertRatingDao();

    public abstract AnswerViewDao answerViewDao();

    public abstract ReplyDao replyDao();

    public abstract HiddenQuestionDao hiddenQuestionDao();

    public abstract CropChartDao cropChartDao();

    public abstract CropChartCommentDao cropChartCommentDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Migration from version 6 to 7: Add hidden_questions table
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the new hidden_questions table
            database.execSQL("CREATE TABLE IF NOT EXISTS `hidden_questions` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + "`expertId` INTEGER NOT NULL, "
                    + "`questionId` INTEGER NOT NULL, " + "`hiddenAt` INTEGER NOT NULL)");
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

    // Migration from version 9 to 10: Add imagePath to expert_answers
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE expert_answers ADD COLUMN imagePath TEXT");
        }
    };

    // Migration from version 11 to 12: Add crop_charts and crop_chart_comments
    // tables
    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `crop_chart_stars` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + "`cropChartId` INTEGER NOT NULL, "
                    + "`expertId` TEXT, " + "`stars` REAL NOT NULL, " + "`createdAt` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create crop_charts table
            database.execSQL("CREATE TABLE IF NOT EXISTS `crop_charts` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + "`farmerId` TEXT, " + "`farmerName` TEXT, "
                    + "`cropName` TEXT, " + "`season` TEXT, " + "`cultivationStartDate` TEXT, "
                    + "`cultivationEndDate` TEXT, " + "`fertilizersUsed` TEXT, " + "`seedCost` REAL NOT NULL, "
                    + "`fertilizerCost` REAL NOT NULL, " + "`laborCost` REAL NOT NULL, "
                    + "`otherCosts` REAL NOT NULL, " + "`totalCost` REAL NOT NULL, " + "`totalYield` REAL NOT NULL, "
                    + "`sellPrice` REAL NOT NULL, " + "`totalRevenue` REAL NOT NULL, " + "`profit` REAL NOT NULL, "
                    + "`isShared` INTEGER NOT NULL, " + "`createdAt` INTEGER NOT NULL, "
                    + "`updatedAt` INTEGER NOT NULL)");

            // Create crop_chart_comments table
            database.execSQL("CREATE TABLE IF NOT EXISTS `crop_chart_comments` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " + "`cropChartId` INTEGER NOT NULL, "
                    + "`expertId` TEXT, " + "`expertName` TEXT, " + "`comment` TEXT, "
                    + "`timestamp` INTEGER NOT NULL)");
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
                    INSTANCE = Room
                            .databaseBuilder(context.getApplicationContext(), AppDatabase.class, "agriminds_database")
                            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_11_12,
                                    MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                                    MIGRATION_17_18)
                            .fallbackToDestructiveMigration().addCallback(sRoomDatabaseCallback)
                            .allowMainThreadQueries().build();

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
