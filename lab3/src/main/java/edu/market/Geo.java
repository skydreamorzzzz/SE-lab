package edu.market;

public final class Geo {
    private Geo() {
    }

    public static void validate(Double latitude, Double longitude) {
        BusinessException.require((latitude == null) == (longitude == null), "请同时提供纬度与经度");
        BusinessException.require(latitude == null || (Double.isFinite(latitude) && Math.abs(latitude) <= 90
                && Double.isFinite(longitude) && Math.abs(longitude) <= 180), "位置坐标无效");
    }

    public static Double distance(Double latitude, Double longitude, Double targetLatitude, Double targetLongitude) {
        if (latitude == null || longitude == null || targetLatitude == null || targetLongitude == null) {
            return null;
        }
        double a = Math.toRadians(latitude);
        double b = Math.toRadians(targetLatitude);
        double cos = Math.sin(a) * Math.sin(b) + Math.cos(a) * Math.cos(b)
                * Math.cos(Math.toRadians(longitude - targetLongitude));
        return Math.round(6371 * Math.acos(Math.max(-1, Math.min(1, cos))) * 10) / 10.0;
    }
}
