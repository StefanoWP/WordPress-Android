package org.wordpress.android.ui.plans;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.plans.models.Feature;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.PhotonUtils;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

public class PlansUtils {

    private static String deviceCurrencyCode; // ISO 4217 currency code.
    private static String deviceCurrencySymbol;

    private static final String DOLLAR_SYMBOL = "$";
    private static final String DOLLAR_ISO4217_CODE = "USD";

    /**
     * Returns the price for the passed plan formatted for the user's locale, defaults
     * to USD if user's locale isn't matched
     *
     * TODO: endpoint will be updated to include the formatted price, so this method is temporary
     */
    public static String getPlanDisplayPrice(@NonNull Plan plan) {
        // lookup currency code/symbol on first use
        if (deviceCurrencyCode == null) {
            Currency currency = Currency.getInstance(Locale.getDefault());
            deviceCurrencyCode = currency.getCurrencyCode();
            deviceCurrencySymbol = currency.getSymbol(Locale.getDefault());
        }

        String currencySymbol;
        int priceValue;
        Hashtable<String, Integer> pricesMap = plan.getPrices();
        if (pricesMap.containsKey(deviceCurrencyCode)) {
            currencySymbol = deviceCurrencySymbol;
            priceValue = pricesMap.get(deviceCurrencyCode);
            return currencySymbol + FormatUtils.formatInt(priceValue);
        } else {
            // locale not found, default to USD
            currencySymbol = DOLLAR_SYMBOL;
            priceValue = pricesMap.get(DOLLAR_ISO4217_CODE);
            return currencySymbol + FormatUtils.formatInt(priceValue) + " " + DOLLAR_ISO4217_CODE;
        }
    }



    @Nullable
    public static Plan getGlobalPlan(long planId) {
        List<Plan> plans = getGlobalPlans();
        if (plans == null || plans.size() == 0) {
            return null;
        }

        for (Plan current: plans) {
            if (current.getProductID() == planId) {
                return  current;
            }
        }

        return null;
    }

    @Nullable
    private static List<Plan> getGlobalPlans() {
        String plansString = AppPrefs.getGlobalPlans();
        if (TextUtils.isEmpty(plansString)) {
            return null;
        }

        List<Plan> plans = new ArrayList<>();
        try {
            JSONObject plansJSONObject = new JSONObject(plansString);
            JSONArray plansArray = plansJSONObject.getJSONArray("originalResponse");
            for (int i=0; i < plansArray.length(); i ++) {
                JSONObject currentPlanJSON = plansArray.getJSONObject(i);
                Plan currentPlan = new Plan(currentPlanJSON);
                plans.add(currentPlan);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.PLANS, "Can't parse the plans list returned from the server", e);
            return null;
        }

        return plans;
    }

    @Nullable
    public static HashMap<String, Feature> getFeatures() {
        String featuresString = AppPrefs.getGlobalPlansFeatures();
        if (TextUtils.isEmpty(featuresString)) {
            return null;
        }

        HashMap<String, Feature> features = new HashMap<>();
        try {
            JSONObject featuresJSONObject = new JSONObject(featuresString);
            JSONArray featuresArray = featuresJSONObject.getJSONArray("originalResponse");
            for (int i=0; i < featuresArray.length(); i ++) {
                JSONObject currentFeatureJSON = featuresArray.getJSONObject(i);
                Feature currentFeature = new Feature(currentFeatureJSON);
                features.put(currentFeature.getProductSlug(), currentFeature);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.PLANS, "Can't parse the features list returned from the server", e);
            return null;
        }

        return features;
    }

    /**
     * Returns the url of the image to display for the passed plan
     *
     * @param planId - ID of the global plan
     * @param iconSize - desired size of the returned image
     * @return string containing photon-ized url for the plan icon
     */
    public static String getIconUrlForPlan(long planId, int iconSize) {
        Plan plan = getGlobalPlan(planId);
        if (plan == null || !plan.hasIconUrl()) {
            return null;
        }
        return PhotonUtils.getPhotonImageUrl(plan.getIconUrl(), iconSize, iconSize);
    }

    /**
     * Compares two plan products - assumes lower product IDs are "lesser" than higher product IDs
     */
    public static final int LESSER_PRODUCT = -1;
    public static final int EQUAL_PRODUCT = 0;
    public static final int GREATER_PRODUCT = 1;
    public static int compareProducts(long lhsProductId, long rhsProductId) {
        // this duplicates Long.compare(), which wasn't added until API 19
        return lhsProductId < rhsProductId ? LESSER_PRODUCT : (lhsProductId == rhsProductId ? EQUAL_PRODUCT : GREATER_PRODUCT);
    }

    /**
     * Removes stored plan data - for testing purposes
     */
    @SuppressWarnings("unused")
    public static void clearPlanData() {
        AppPrefs.setGlobalPlans(null);
        AppPrefs.setGlobalPlansFeatures(null);
    }

}