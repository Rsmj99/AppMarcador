package com.example.sesion14_ejemplo01;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.example.sesion14_ejemplo01.db.DBHelper;
import com.example.sesion14_ejemplo01.modelo.Marcador;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.sesion14_ejemplo01.databinding.ActivityMapsBinding;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, CuadroRegEdiTitulo.FinalizoCuadroTitulo, CuadroBotones.FinalizoCuadroBotones {

    private GoogleMap mMap;
    //    private ActivityMapsBinding binding;
    private LocationManager mlocManager;
    private Preferences preferences;
    private DBHelper dbHelper;
    private Marker markerUbicacion;
    private Marcador marcador;
    private int contar = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DBHelper(getApplicationContext());
        /*binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());*/
        setContentView(R.layout.activity_maps);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        preferences = new Preferences(getBaseContext());
        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        ArrayList<Marcador> marcadores = dbHelper.Traer_Marcadores();
        for (int i=0; i<marcadores.size(); i++){
            LatLng latLng = new LatLng(marcadores.get(i).getLatitud(), marcadores.get(i).getLongitud());
            mMap.addMarker(new MarkerOptions().position(latLng).title(marcadores.get(i).getTitulo()).draggable(true));
        }

        // Add a marker in Sydney and move the camera
        /*LatLng latLng = new LatLng(-11.108722, -77.610240);
        mMap.addMarker(new MarkerOptions().position(latLng).title("Plaza de Armas de Huacho"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));*/

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Location location = new Location("");
                location.setLatitude(latLng.latitude);
                location.setLongitude(latLng.longitude);
                ActualizarUbicacion(location);
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marcador = dbHelper.Traer_Marcador(marker.getTitle());
                new CuadroBotones(MapsActivity.this, MapsActivity.this, marker);
                return false;
            }
        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                new CuadroRegEdiTitulo(MapsActivity.this, MapsActivity.this, latLng);
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                marker.hideInfoWindow();
                marcador = dbHelper.Traer_Marcador(marker.getTitle());
            }

            @Override
            public void onMarkerDrag(Marker marker) {}

            @Override
            public void onMarkerDragEnd(Marker marker) {
                marcador.setLatitud(marker.getPosition().latitude);
                marcador.setLongitud(marker.getPosition().longitude);
                dbHelper.Actualizar_Marcador(marcador);
                marker.showInfoWindow();
            }
        });

        locationStart();
    }

    private void locationStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return;
        }
        Location location = mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
        mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, locationListener);
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (contar == 0) {
//                Toast.makeText(MapsActivity.this, "Cambio", Toast.LENGTH_SHORT).show();
                ActualizarUbicacion(location);
            } else {
                contar--;
            }
        }
    };

    private void ActualizarUbicacion(Location location) {
        if (location != null) {
            LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
            if (markerUbicacion != null) {
                markerUbicacion.remove();
            }
            markerUbicacion = mMap.addMarker(new MarkerOptions().position(myLocation).title("Mi Ubicación").draggable(true));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17));
            preferences.putLatitud(location.getLatitude() + "");
            preferences.putLongitud(location.getLongitude() + "");
        }
    }

    @Override
    public void ResultadoCuadroTitulo(LatLng latLng, String titulo) {
        Marcador marcador = new Marcador(latLng.latitude, latLng.longitude, titulo);
        dbHelper.Insertar_Marcador(marcador);
        mMap.addMarker(new MarkerOptions().position(latLng).title(titulo).draggable(true));
    }

    @Override
    public void ResultadoCuadroBotones(LatLng latLng, String titulo) {
        marcador.setLatitud(latLng.latitude);
        marcador.setLongitud(latLng.longitude);
        marcador.setTitulo(titulo);
        dbHelper.Actualizar_Marcador(marcador);
        mMap.addMarker(new MarkerOptions().position(latLng).title(titulo).draggable(true));
        marcador = null;
    }
}