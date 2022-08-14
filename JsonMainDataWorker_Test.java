package com.package.xxx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.package.xxx.R;
import com.package.xxx.activity.MainActivity;
import com.package.xxx.database.DietSelectedDatabase;
import com.package.xxx.database.ExerciseDatabase;
import com.package.xxx.database.FoodDatabase;
import com.package.xxx.database.MainItemDatabase;
import com.package.xxx.database.RecipeSelectedDatabase;
import com.package.xxx.database.SliderDatabase;
import com.package.xxx.database.UserRecipeDatabase;
import com.package.xxx.database.dao.DietSelectedDao;
import com.package.xxx.database.dao.ExerciseDao;
import com.package.xxx.database.dao.FoodDao;
import com.package.xxx.database.dao.MainDao;
import com.package.xxx.database.dao.RecipeSelectedDao;
import com.package.xxx.database.dao.SliderDao;
import com.package.xxx.database.dao.UserRecipeDao;
import com.package.xxx.database.entities.DietSelectedCategory;
import com.package.xxx.database.entities.Exercise;
import com.package.xxx.database.entities.Food;
import com.package.xxx.database.entities.FoodUnits;
import com.package.xxx.database.entities.RecipeSelectedCategory;
import com.package.xxx.model.FoodArray;
import com.package.xxx.model.MainItemModel;
import com.package.xxx.model.SliderModel;
import com.package.xxx.model.UploadRecipeModel;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class JsonMainDataWorker extends Worker {
    public Context context;
    public final String TAG = "JsonDataWorker";
    public String append;
    public SliderDao sliderDao;
    public UserRecipeDao userRecipeDao;
    public MainDao mainItemDao;
    public DietSelectedDao dietSelectedDao;
    public RecipeSelectedDao recipeSelectedDao;
    public FoodDao foodDao;
    public ExerciseDao exerciseDao;
    public List<FoodArray> foodsArrayList;
    public List<Food> foodsDataList;
    public List<Exercise> exerciseList;
    public long system_time;
    public int all_items_size = 0;
    public int recipe_items_size = 0;
    public int slide_items_size = 0;
    public int food_items_size = 0;
    public int exercise_items_size = 0;
    public int activity_items_temp_size = 0;
    public int food_items_temp_size = 0;
    public List<UploadRecipeModel> userTariflerItemsList;
    public List<SliderModel> sliderItems;
    public List<MainItemModel> fullItemList;
    public boolean user_recipe_update = false;
    public boolean food_db_update = false;
    public boolean activity_db_update = false;
    public DatabaseReference databaseReference;
    public boolean getStatus=false;
    public  Callback callback;

    public JsonMainDataWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork");
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference =
                database.getReference("PATH").child("CHILD");

        context = getApplicationContext();
        userTariflerItemsList = new ArrayList<>();
        sliderItems = new ArrayList<>();
        fullItemList = new ArrayList<>();
        foodsArrayList = new ArrayList<>();
        foodsDataList = new ArrayList<>();
        exerciseList = new ArrayList<>();
        try {
            databaseExecutor();
            return  Result.success();
        } catch (Exception exception) {
            Log.d(TAG, "" + exception);
            return  Result.failure();
        }
    }



    public void databaseExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            SliderDatabase sliderDatabase = SliderDatabase.getDatabaseInstance(context);
            sliderDao = sliderDatabase.sliderDao();
            UserRecipeDatabase userRecipeDatabase = UserRecipeDatabase.getDatabaseInstance(context);
            userRecipeDao = userRecipeDatabase.recipeDao();
            MainItemDatabase mainItemDatabase = MainItemDatabase.getDatabaseInstance(context);
            mainItemDao = mainItemDatabase.mainItemDao();
            DietSelectedDatabase selectedDatabase = DietSelectedDatabase.getDatabase(context);
            RecipeSelectedDatabase recipeSelectedDatabase =
                    RecipeSelectedDatabase.getDatabase(context);

            dietSelectedDao = selectedDatabase.dao();
            recipeSelectedDao = recipeSelectedDatabase.dao();

            handler.post(this::getDataSize);
        });
    }

    public void getDataSize() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                Constant.DATA_SIZE, null,
                response -> {
                    long baslangic = System.currentTimeMillis();
                    try {
                        JSONArray jsonArray = response.getJSONArray("JSONARRAY");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject object = jsonArray.getJSONObject(i);
                            all_items_size = object.getInt("all_items_size");
                            food_items_size = object.getInt("food_items_size");
                            recipe_items_size = object.getInt("recipe_items_size");
                            slide_items_size = object.getInt("slide_items_size");
                            exercise_items_size = object.getInt("activity_items_size");
                        }

                        Log.d(TAG, "userTariflerItemsSize: " + userTariflerItemsList.size());

                        if (SharedPreferencesReceiver.getRecipeSize(context) != recipe_items_size) {
                            user_recipe_update = true;
                        }
                        if (SharedPreferencesReceiver.getFoodDataSize(context) != food_items_size) {
                            food_db_update = true;
                            food_items_temp_size = food_items_size;
                        }

                        if (SharedPreferencesReceiver.getActivityDataSize(context) != exercise_items_size) {
                            activity_db_update = true;
                            activity_items_temp_size = exercise_items_size;
                        }

                        dataSizeParse();

                        Log.d(TAG, "userTariflerItemsSize: " + userTariflerItemsList.size());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d(TAG, "JSON Exception:" + e);
                    }
                    long bitis = System.currentTimeMillis();
                    Log.d(TAG, "getDataSize_Request " + (bitis - baslangic));
                }, this::exceptionMessage);
        jsonObjectRequest.setShouldCache(false);
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MILLISSECOND, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES));
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(jsonObjectRequest);
    }



    public void dataSizeParse() {
        if (SharedPreferencesReceiver.getSlideSize(context) != slide_items_size) {
            sliderDataRequest();
        } else if (SharedPreferencesReceiver.getAllDataSize(context) != all_items_size) {
            dataRequest();
        } else {
            if (user_recipe_update) {
                readData(modelList -> {
                    Log.d(TAG, "userRecipeModelList: " + modelList.size());
                    new InsertUserData().execute(modelList);
                });
            } else if (food_db_update) {
                foodDatabase();
            } else if (activity_db_update) {
                activityDatabase();
            } else {
                Log.d(TAG, "work Finished!");
                getStatus=true;

                // Finished Process


                // SharedPreferencesReceiver.setDataCheck(context, true);
              //  Intent intent = new Intent(getApplicationContext(), MainActivity.class);
               // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               // context.startActivity(intent);
            }

        }
    }

    public void foodDatabase() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            FoodDatabase database = FoodDatabase.getDatabaseInstance(context);
            foodDao = database.daoAccess();
            foodDao.deleteTable();
            foodDao.clearPrimaryKey();
            handler.post(this::getFoodRequest);
        });

    }

    public void activityDatabase() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            ExerciseDatabase exerciseDatabase = ExerciseDatabase.getDatabaseInstance(context);
            exerciseDao = exerciseDatabase.daoAccess();
            exerciseDao.deleteTable();
            exerciseDao.clearPrimaryKey();


            handler.post(() -> getExerciseRequest());
        });

    }

    public void getFoodRequest() {
        foodsArrayList.clear();
        system_time = System.currentTimeMillis();
        Log.d(TAG, "FoodDB_Time: " + system_time);
        @SuppressLint("SetJavaScriptEnabled") JsonObjectRequest jsonObjectRequest =
                new JsonObjectRequest(Request.Method.GET,
                        Constant.FOOD_LIST, null,
                        response -> {
                            long start = System.currentTimeMillis();
                            try {
                                JSONArray jsonArray = response.getJSONArray("JSONARRAY");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    FoodArray foodArray = new FoodArray();

                                    List<FoodUnits> foodUnitList = new ArrayList<>();
                                    Food foodsData = new Food();

                                    foodsData.setFoodId(object.getInt("food_id"));
                                    foodsData.setFoodName(object.getString("food_name"));
                                    foodsData.setFoodImage(object.getString("food_image"));
                                    foodsData.setFoodUrl(object.getString("food_url"));
                                    foodsData.setFoodKcal(object.getString("food_kcal"));
                                    foodsData.setFoodDescription(object.getString("food_description"));
                                    foodsData.setMealTime(object.getString("meal_time"));
                                    foodsData.setFood_first_unit(object.getString("food_first_unit"));
                                    foodsData.setCarbPercent(object.getString("carb_percent"));
                                    foodsData.setProteinPercent(object.getString("protein_percent"));
                                    foodsData.setFatPercent(object.getString("fat_percent"));
                                    foodsData.setTimeStamp(system_time - (i + 1));
                                    foodsData.setUser(false);

                                    JSONArray jsonUnitsArray = object.getJSONArray("units");

                                    for (int k = 0; k < jsonUnitsArray.length(); k++) {
                                        JSONObject unitobject = jsonUnitsArray.getJSONObject(k);

                                        FoodUnits foodUnitData = new FoodUnits();
                                        foodUnitData.setUnit(unitobject.getString("unit"));
                                        foodUnitData.setAmount(unitobject.getString("amount"));
                                        foodUnitData.setCalory(unitobject.getString("calory"));
                                        foodUnitData.setCarbohydrt(unitobject.getString("carbohydrt"));
                                        foodUnitData.setProtein(unitobject.getString("protein"));
                                        foodUnitData.setFat(unitobject.getString("fat"));
                                        foodUnitData.setCalcium(unitobject.getString("calcium"));
                                        foodUnitData.setCholestrl(unitobject.getString("cholestrl"));
                                        foodUnitData.setFiberTd(unitobject.getString("fiber_td"));
                                        foodUnitData.setIron(unitobject.getString("iron"));
                                        foodUnitData.setLipidTot(unitobject.getString("lipid_tot"));
                                        foodUnitData.setPotassium(unitobject.getString("potassium"));
                                        foodUnitData.setSodium(unitobject.getString("sodium"));
                                        foodUnitData.setVitAIu(unitobject.getString("vit_a_iu"));
                                        foodUnitData.setVitC(unitobject.getString("vit_c"));
                                        foodUnitData.setVitC(unitobject.getString("vit_c"));
                                        foodUnitList.add(foodUnitData);
                                    }
                                    foodArray.setFoods(foodsData);
                                    foodArray.setFoodUnits(foodUnitList);
                                    foodsArrayList.add(foodArray);
                                    foodsDataList.add(foodsData);
                                }
                                new InsertAsync(foodsArrayList).execute();


                                long finish = System.currentTimeMillis();
                                Log.d(TAG, "FoodDB_RequestTime: " + (finish - start));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.d(TAG, "FoodDB_JSONException: " + e);
                            }
                        }, error -> {
                    Log.d(TAG, "FoodDB_JSONException_String: " + error.toString());
                });
        jsonObjectRequest.setShouldCache(false);
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MILLISSECOND, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES));
        RequestQueue request_queue = Volley.newRequestQueue(context);
        request_queue.add(jsonObjectRequest);
    }


    @SuppressLint("StaticFieldLeak")
    class InsertAsync extends AsyncTask<String, Void, Integer> {
        List<FoodArray> foodsArrayList;


        InsertAsync(List<FoodArray> foodsArrayList) {
            this.foodsArrayList = foodsArrayList;
        }

        @SafeVarargs
        protected final Integer doInBackground(String... arg0) {
            for (int i = 0; i < foodsArrayList.size(); i++) {
                foodDao.insertAll(foodsArrayList.get(i));
            }

            return foodDao.getAll().size();
        }

        protected void onPostExecute(Integer result) {
            setFoodBlink(result);
            super.onPostExecute(result);
        }
    }

    public void setFoodBlink(int result) {
        SharedPreferencesReceiver.setFoodDataSize(context, result);
        Log.d(TAG, "FoodDB_Result: " + result);
        Log.d(TAG, "FoodDB_List: " + food_items_size);
        if (food_items_size == result) {
            Log.d(TAG, "FoodDB_TRUE");
            food_db_update = false;
        } else {
            Log.d(TAG, "FoodDB_FALSE");
            food_db_update = true;
        }
        dataSizeParse();
    }

    public void getExerciseRequest() {
        foodsArrayList.clear();
        system_time = System.currentTimeMillis();
        Log.d(TAG, "ExerciseJSON_Time" + system_time);
        @SuppressLint("SetJavaScriptEnabled") JsonObjectRequest jsonObjectRequest =
                new JsonObjectRequest(Request.Method.GET,
                        Constant.EXERCISE_LIST, null,
                        response -> {
                            long start = System.currentTimeMillis();
                            try {
                                JSONArray jsonArray = response.getJSONArray("JSONARRAY");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    Exercise exercise = new Exercise();

                                    exercise.setExerciseId(object.getInt("exercise_id"));
                                    exercise.setExerciseName(object.getString("exercise_name"));
                                    exercise.setExerciseImage(object.getString("exercise_image"));
                                    exercise.setExerciseFactor((float) object.getDouble("exercise_factor"));
                                    exercise.setExerciseWeightFactor(object.getInt("exercise_weight_factor"));
                                    exercise.setExerciseMinutes(object.getString("exercise_minutes"));
                                    exercise.setExerciseTargetedMuscle(object.getString("exercise_targeted_muscle"));
                                    exercise.setExerciseDesc(object.getString("exercise_desc"));
                                    exercise.setTimeStamp(system_time - (i + 1));
                                    exercise.setUser(false);

                                    exerciseList.add(exercise);
                                }
                                new InsertAsyncExercise(exerciseList).execute();


                                long finish = System.currentTimeMillis();
                                Log.d(TAG, "ExerciseJSON_Request: " + (finish - start));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.d(TAG, "ExerciseJSON_Exception:" + e);
                            }
                        }, error -> Log.d(TAG, "ExerciseJSON_Exception_String: " + error.toString()));
        jsonObjectRequest.setShouldCache(false);
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MILLISSECOND, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES));
        RequestQueue request_queue = Volley.newRequestQueue(context);
        request_queue.add(jsonObjectRequest);
    }


    @SuppressLint("StaticFieldLeak")
    class InsertAsyncExercise extends AsyncTask<String, Void, Integer> {
        List<Exercise> exerciseList;


        InsertAsyncExercise(List<Exercise> exerciseList) {
            this.exerciseList = exerciseList;
        }

        @SafeVarargs
        protected final Integer doInBackground(String... arg0) {
            for (int i = 0; i < exerciseList.size(); i++) {
                exerciseDao.insertData(exerciseList.get(i));
            }

            return exerciseDao.getAllExercises().size();
        }

        protected void onPostExecute(Integer result) {
            setExerciseBlink(result);
            super.onPostExecute(result);
        }
    }


    public void setExerciseBlink(int result) {
        SharedPreferencesReceiver.setActivityDataSize(context, result);
        Log.d(TAG, "ExerciseResult: " + result);
        Log.d(TAG, "ExerciseList: " + exercise_items_size);
        if (exercise_items_size == result) {
            Log.d(TAG, "ActivityDB_TRUE");
            activity_db_update = false;
        } else {
            Log.d(TAG, "ActivityDB_FALSE");
            activity_db_update = true;
        }
        dataSizeParse();
    }


    public void deleteUserData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            userRecipeDao.deleteTable();
            userRecipeDao.clearPrimaryKey();
        });
    }

    @SuppressLint("StaticFieldLeak")
    class InsertUserData extends AsyncTask<List<UploadRecipeModel>, Void, Integer> {
        @SafeVarargs
        protected final Integer doInBackground(List<UploadRecipeModel>... arg0) {
            for (int i = 0; i < arg0[0].size(); i++) {
                userRecipeDao.insertData(arg0[0].get(i));
            }

            return userRecipeDao.getAllData().size();
        }

        protected void onPostExecute(Integer result) {
            setRecipeSize(result);
            super.onPostExecute(result);
        }
    }

    public void setRecipeSize(int result) {
        SharedPreferencesReceiver.setRecipeSize(context, result);
        user_recipe_update = false;
        Log.d(TAG, "RecipeSize: " + result);
        dataSizeParse();
    }
    // User Recipe  //

    private interface FirebaseCallback {
        void onCallback(List<UploadRecipeModel> list);
    }


    public void readData(FirebaseCallback firebaseCallback) {
        deleteUserData();
        userTariflerItemsList.clear();
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    UploadRecipeModel recipeModel = data.getValue(UploadRecipeModel.class);
                    userTariflerItemsList.add(recipeModel);
                }
                firebaseCallback.onCallback(userTariflerItemsList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    // Slider  //

    public void sliderDataRequest() {
        deleteSliderData();
        sliderItems.clear();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                Constant.SLIDER_ITEMS, null,
                response -> {
                    long baslangic = System.currentTimeMillis();
                    try {
                        JSONArray jsonArray = response.getJSONArray("JSONARRAY");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject object = jsonArray.getJSONObject(i);
                            SliderModel sliderItemData = new SliderModel();
                            sliderItemData.setId(object.getInt("id"));
                            sliderItemData.setImage(object.getString("image"));
                            sliderItemData.setTitle(object.getString("title"));
                            sliderItemData.setDesc(object.getString("desc"));
                            sliderItemData.setTag(object.getString("tag"));
                            sliderItemData.setRelated(object.getString("related"));
                            sliderItemData.setContent(object.getString("content"));
                            sliderItemData.setUrl(object.getString("url"));
                            sliderItems.add(sliderItemData);
                        }
                        new InsertSliderData().execute(sliderItems);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d(TAG, "SliderJSON_Exception: " + e);
                    }
                    long bitis = System.currentTimeMillis();
                    Log.d(TAG, "SliderJSON_Request" + (bitis - baslangic));
                }, error -> {
            Log.d(TAG, "SliderJSON_Exception_String" + error.toString());
            exceptionMessage(error);
        });
        jsonObjectRequest.setShouldCache(false);
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MILLISSECOND, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES));
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(jsonObjectRequest);
    }

    public void deleteSliderData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            sliderDao.deleteTable();
            sliderDao.clearPrimaryKey();
        });
    }

    public class InsertSliderData extends AsyncTask<List<SliderModel>, Void, Integer> {
        @SafeVarargs
        protected final Integer doInBackground(List<SliderModel>... arg0) {
            for (int i = 0; i < arg0[0].size(); i++) {
                sliderDao.insertSlider(arg0[0].get(i));
            }

            return sliderDao.getAllData().size();
        }

        protected void onPostExecute(Integer result) {
            SharedPreferencesReceiver.setSlideSize(context, result);
            Log.d(TAG, "SliderResult: " + result);
            dataSizeParse();
            super.onPostExecute(result);
        }
    }


    // Slider  //


    // Data Request  //


    public void dataRequest() {
        deleteMainData();
        if (SharedPreferencesReceiver.getFirstFilter(context)) {
            diyetlerKategoriList();
        } else {
            mainDataRequest();
        }
    }


    public void mainDataRequest() {
        fullItemList.clear();
        RequestQueue main_queue = Volley.newRequestQueue(context);
        main_queue.getCache().clear();
        @SuppressLint("NotifyDataSetChanged") JsonObjectRequest jsonObjectRequest =
                new JsonObjectRequest(Request.Method.GET,
                        Constant.MAIN_ITEMS, null,
                        response -> {
                            long baslangic = System.currentTimeMillis();
                            try {
                                JSONArray jsonArray = response.getJSONArray("JSONARRAY");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    MainItemModel diyetItemModelFull = new MainItemModel();
                                    diyetItemModelFull.setId(object.getInt("diyet_id"));
                                    diyetItemModelFull.setDiyet_adi(object.getString("diyet_adi"));
                                    diyetItemModelFull.setDiyet_resim(object.getString("diyet_resim"));
                                    diyetItemModelFull.setDiyet_url(object.getString("diyet_url"));
                                    diyetItemModelFull.setDiyet_tipi(object.getString("diyet_tipi"));
                                    diyetItemModelFull.setKategori_adi(object.getString("kategori_adi"));
                                    diyetItemModelFull.setKategori_adi_ek(object.getString("kategori_adi_ek"));
                                    diyetItemModelFull.setDiyet_yazar(object.getString("diyet_yazar"));
                                    fullItemList.add(diyetItemModelFull);
                                }

                                Log.d(TAG, "MainDataFull: " + fullItemList.size());


                                new InsertAllData().execute(fullItemList);


                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.d(TAG, "MainData_JSONException:" + e);
                            }

                            long bitis = System.currentTimeMillis();
                            Log.d(TAG, "MainData_JSON_Request:" + (bitis - baslangic));
                        }, this::exceptionMessage);
        jsonObjectRequest.setShouldCache(false);
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MILLISSECOND, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES));
        main_queue.add(jsonObjectRequest);
    }


    public class InsertAllData extends AsyncTask<List<MainItemModel>, Void, Integer> {
        @SafeVarargs
        protected final Integer doInBackground(List<MainItemModel>... arg0) {
            for (int i = 0; i < arg0[0].size(); i++) {
                mainItemDao.insertData(arg0[0].get(i));
            }
            return mainItemDao.getAllData().size();
        }

        protected void onPostExecute(Integer result) {
            SharedPreferencesReceiver.setAllDataSize(context, result);
            Log.d("MainData_Result", "result: " + result);
            dataSizeParse();
            super.onPostExecute(result);
        }
    }


    public void deleteMainData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            mainItemDao.deleteTable();
            mainItemDao.clearPrimaryKey();
        });
    }


    public void diyetlerKategoriList() {
        RequestQueue main_queue = Volley.newRequestQueue(context);
        main_queue.getCache().clear();
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                Constant.DIET_CATEGORY,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.getJSONArray("JSONARRAY");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject object = jsonArray.getJSONObject(i);
                            dietSelectedDao.insert(new DietSelectedCategory(object.getInt("id"), object.getString("kategori_adi")));
                        }
                        tariflerKategoriList();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d(TAG, "DiyetListeleriException:" + e);
                    }
                }, error -> {
            exceptionMessage(error);
            Log.d(TAG, "DiyetListeleriException_String" + error.toString());
        });
        stringRequest.setShouldCache(false);

        main_queue.add(stringRequest);
    }

    public void tariflerKategoriList() {
        RequestQueue main_queue = Volley.newRequestQueue(context);
        main_queue.getCache().clear();
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                Constant.RECIPE_CATEGORY,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.getJSONArray("JSONARRAY");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject object = jsonArray.getJSONObject(i);
                            recipeSelectedDao.insert(new RecipeSelectedCategory(object.getInt("id"), object.getString("kategori_adi")));
                        }

                        SharedPreferencesReceiver.setFirstFilter(context, false);
                        dataRequest();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {
            exceptionMessage(error);
            Log.d(TAG, "RecipeJSONException: " + error.toString());
        });
        stringRequest.setShouldCache(false);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MILLISSECOND, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES));
        main_queue.add(stringRequest);

    }


//// food db


    public void exceptionMessage(VolleyError error) {
        try {
            if (error instanceof NoConnectionError) {
                append = context.getString(R.string.check_your_internet);
            } else if (error.getCause() instanceof MalformedURLException) {
                append = context.getString(R.string.bad_request);
            } else if (error instanceof ParseError || error.getCause() instanceof IllegalStateException
                    || error.getCause() instanceof JSONException
                    || error.getCause() instanceof XmlPullParserException) {
                append = context.getString(R.string.parse_error);
            } else if (error.getCause() instanceof OutOfMemoryError) {
                append = context.getString(R.string.out_of_memory);
            } else if (error instanceof AuthFailureError) {
                append = context.getString(R.string.authenticated_error);
            } else if (error instanceof ServerError || error.getCause() instanceof ServerError) {
                append = context.getString(R.string.server_isnt_responding);
            } else if (error instanceof TimeoutError || error.getCause() instanceof SocketTimeoutException
                    || error.getCause() instanceof ConnectTimeoutException
                    || error.getCause() instanceof SocketException
                    || (Objects.requireNonNull(error.getCause()).getMessage() != null
                    && Objects.requireNonNull(error.getCause().getMessage()).contains(context.getString(R.string.connection_timeout)))) {
                append = context.getString(R.string.connection_timeout);
            } else {
                append = context.getString(R.string.an_uknown_error);
            }
        } finally {
            SharedPreferencesReceiver.setDataCheck(context, false);
        }

    }

}
