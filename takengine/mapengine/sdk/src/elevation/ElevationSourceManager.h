#ifndef TAK_ENGINE_ELEVATION_ELEVATIONSOURCEMANAGER_H_INCLUDED
#define TAK_ENGINE_ELEVATION_ELEVATIONSOURCEMANAGER_H_INCLUDED

#include "elevation/ElevationSource.h"
#include "port/Collection.h"
#include "port/Platform.h"
#include "util/Error.h"

namespace TAK {
    namespace Engine {
        namespace Elevation {
            class ElevationSourcesChangedListener
            {
            public :
                virtual ~ElevationSourcesChangedListener() NOTHROWS = 0;
            public :
                virtual Util::TAKErr onSourceAttached(const std::shared_ptr<ElevationSource> &src) NOTHROWS = 0;
                virtual Util::TAKErr onSourceDetached(const ElevationSource &src) NOTHROWS = 0;
            };

            Util::TAKErr ElevationSourceManager_addOnSourcesChangedListener(ElevationSourcesChangedListener *listener) NOTHROWS;
            Util::TAKErr ElevationSourceManager_removeOnSourcesChangedListener(ElevationSourcesChangedListener *listener) NOTHROWS;
            Util::TAKErr ElevationSourceManager_attach(const std::shared_ptr<ElevationSource> &source) NOTHROWS;
            Util::TAKErr ElevationSourceManager_detach(const ElevationSource &source) NOTHROWS;
            Util::TAKErr ElevationSourceManager_findSource(std::shared_ptr<ElevationSource> &value, const char *name) NOTHROWS;
            Util::TAKErr ElevationSourceManager_getSources(Port::Collection<std::shared_ptr<ElevationSource>> &value) NOTHROWS;
        }
    }
}

#endif
