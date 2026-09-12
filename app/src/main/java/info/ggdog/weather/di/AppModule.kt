package info.ggdog.weather.di

import info.ggdog.weather.data.repository.LocationRepositoryImpl
import info.ggdog.weather.data.repository.WeatherRepositoryImpl
import info.ggdog.weather.domain.repository.LocationRepository
import info.ggdog.weather.domain.repository.WeatherRepository
import dagger.Module
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository

    @Binds
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
}
