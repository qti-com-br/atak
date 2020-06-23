#include "feature/FeatureDataSource2.h"

#include <map>
#include <set>

#include "feature/FeatureDataSource.h"
#include "feature/LegacyAdapters.h"
#include "thread/RWMutex.h"
#include "util/Memory.h"

using namespace TAK::Engine::Feature;

using namespace TAK::Engine::Thread;
using namespace TAK::Engine::Util;

namespace
{
    typedef std::map<TAK::Engine::Port::String, std::shared_ptr<FeatureDataSource2>, TAK::Engine::Port::StringLess> FeatureDataSourceMap;

    RWMutex &providerMapMutex() NOTHROWS;
    FeatureDataSourceMap& getProviderMap() NOTHROWS;
}

FeatureDataSource2::~FeatureDataSource2() NOTHROWS
{}

FeatureDataSource2::Content::~Content() NOTHROWS
{}

TAKErr TAK::Engine::Feature::FeatureDataSourceFactory_parse(FeatureDataSource2::ContentPtr &content, const char *path, const char *hint) NOTHROWS
{
    TAKErr code(TE_Ok);
    ReadLockPtr lock(NULL, NULL);
    code = ReadLock_create(lock, providerMapMutex());
    TE_CHECKRETURN_CODE(code);
    // return ContentPtr for current FeatureDataSource implemenations
    FeatureDataSourceMap featureDataSourceMap(getProviderMap());
    FeatureDataSourceMap::iterator it;
    if (hint) {
        std::shared_ptr<FeatureDataSource2> spi;

        // look up provider directly if a hint is specified
        it = featureDataSourceMap.find(hint);
        if (it != featureDataSourceMap.end())
        {
            spi = it->second;
            if (spi.get() != nullptr)
                return spi->parse(content, path);
        }
    } else {
        // iterate all available providers. choose the first that supports
        for (it = featureDataSourceMap.begin(); it != featureDataSourceMap.end(); it++)
        {
            TAKErr code = it->second->parse(content, path);
            if (code == TE_Ok)
                return code;
        }
    }

    // fall-through to legacy

    // return ContentPtr for legacy FeatureDataSource implemenations
    std::unique_ptr<atakmap::feature::FeatureDataSource::Content, void(*)(const atakmap::feature::FeatureDataSource::Content *)> parsed(NULL, NULL);
    try {
        parsed = std::unique_ptr<atakmap::feature::FeatureDataSource::Content, void(*)(const atakmap::feature::FeatureDataSource::Content *)>(atakmap::feature::FeatureDataSource::parse(path, hint), Memory_deleter_const<atakmap::feature::FeatureDataSource::Content>);
    } catch (...) {}
    if (!parsed.get())
        return TE_InvalidArg;
    return LegacyAdapters_adapt(content, std::move(parsed));
}

TAKErr TAK::Engine::Feature::FeatureDataSourceFactory_getProvider(std::shared_ptr<FeatureDataSource2> &spi, const char *hint) NOTHROWS
{
    if (!hint)
        return TE_InvalidArg;

    TAKErr code(TE_Ok);
    ReadLockPtr lock(NULL, NULL);
    code = ReadLock_create(lock, providerMapMutex());
    TE_CHECKRETURN_CODE(code);

    // check if feature data source can found amongst current implementations
    FeatureDataSourceMap featureDataSourceMap(getProviderMap());
    FeatureDataSourceMap::iterator it = featureDataSourceMap.find(hint);
    if (it != featureDataSourceMap.end())
    {
        spi = it->second;
    }
    else
    {
        // check legacy feature data source implemenations
        static std::map<const atakmap::feature::FeatureDataSource *, std::shared_ptr<FeatureDataSource2>> legacyToAdapted;
        static Mutex mutex; // guards 'legacyToAdapted' and 'adaptedSpis'

        const atakmap::feature::FeatureDataSource *legacy = atakmap::feature::FeatureDataSource::getProvider(hint);
        if (!legacy)
            return TE_InvalidArg;

        TAKErr code(TE_Ok);
        LockPtr lock(NULL, NULL);
        code = Lock_create(lock, mutex);
        TE_CHECKRETURN_CODE(code);

        std::map<const atakmap::feature::FeatureDataSource *, std::shared_ptr<FeatureDataSource2>>::iterator entry;
        entry = legacyToAdapted.find(legacy);
        if (entry != legacyToAdapted.end()) {
            spi = entry->second;
            return TE_Ok;
        }

        FeatureDataSourcePtr adapter(NULL, NULL);
        code = LegacyAdapters_adapt(adapter, *legacy);
        TE_CHECKRETURN_CODE(code);

        spi = std::move(adapter);
        legacyToAdapted[legacy] = spi;
    }
    return TE_Ok;
}

TAKErr TAK::Engine::Feature::FeatureDataSourceFactory_registerProvider(FeatureDataSourcePtr &&provider_, const int priority) NOTHROWS
{
    std::shared_ptr<FeatureDataSource2> provider = std::move(provider_);
    return FeatureDataSourceFactory_registerProvider(provider, priority);
}

TAKErr TAK::Engine::Feature::FeatureDataSourceFactory_registerProvider(const std::shared_ptr<FeatureDataSource2> &provider, const int priority) NOTHROWS
{
    if (!provider.get())
        return TE_InvalidArg;

    TAKErr code(TE_Ok);
    WriteLockPtr lock(NULL, NULL);
    code = WriteLock_create(lock, providerMapMutex());
    TE_CHECKRETURN_CODE(code);

    FeatureDataSourceMap &featureDataSourceMap = getProviderMap();
    featureDataSourceMap[provider->getName()] = provider;

    return TE_Ok;
}

TAKErr TAK::Engine::Feature::FeatureDataSourceFactory_unregisterProvider(const FeatureDataSource2 &provider) NOTHROWS
{
    TAKErr code(TE_Ok);
    WriteLockPtr lock(NULL, NULL);
    code = WriteLock_create(lock, providerMapMutex());
    TE_CHECKRETURN_CODE(code);

    FeatureDataSourceMap &featureDataSourceMap = getProviderMap();
    FeatureDataSourceMap::iterator entry;
    for (entry = featureDataSourceMap.begin(); entry != featureDataSourceMap.end(); entry++) {
        if (entry->second.get() == &provider) {
            featureDataSourceMap.erase(entry);
            return TE_Ok;
        }
    }

    return TE_InvalidArg;
}

namespace
{
    RWMutex &providerMapMutex() NOTHROWS
    {
        static RWMutex providerMapMutex;
        return providerMapMutex;
    }

    FeatureDataSourceMap& getProviderMap() NOTHROWS
    {
        static FeatureDataSourceMap providerMap;
        return providerMap;
    }
}
