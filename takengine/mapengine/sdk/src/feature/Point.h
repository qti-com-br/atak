////============================================================================
////
////    FILE:           Point.h
////
////    DESCRIPTION:    Concrete class representing a 2D or 3D point.
////

////
////
////    HISTORY:
////
////      DATE          AUTHOR          COMMENTS
////      ------------  --------        --------
////      Dec 12, 2014
////
////========================================================================////
////                                                                        ////
////    (c) Copyright 2014 PAR Government Systems Corporation.              ////
////                                                                        ////
////========================================================================////


#ifndef ATAKMAP_FEATURE_POINT_H_INCLUDED
#define ATAKMAP_FEATURE_POINT_H_INCLUDED


////========================================================================////
////                                                                        ////
////    INCLUDES AND MACROS                                                 ////
////                                                                        ////
////========================================================================////


#include <cstddef>
#include <stdexcept>
#include <stdint.h>
#include <vector>

#include "math/Point.h"
#include "feature/Geometry.h"
#include "port/Platform.h"


////========================================================================////
////                                                                        ////
////    FORWARD DECLARATIONS                                                ////
////                                                                        ////
////========================================================================////

////========================================================================////
////                                                                        ////
////    TYPE DEFINITIONS                                                    ////
////                                                                        ////
////========================================================================////


namespace atakmap                       // Open atakmap namespace.
{
namespace feature                       // Open feature namespace.
{


///=============================================================================
///
///  class atakmap::feature::Point
///
///     A 2D or 3D point.
///
///=============================================================================


class ENGINE_API Point
  : public Geometry,
    public math::Point<double>
  {
                                        //====================================//
  public:                               //                      PUBLIC        //
                                        //====================================//

    explicit
    Point (Dimension dim)
      : Geometry (Geometry::POINT, dim)
      { }

    Point (double x,
           double y)
      : Geometry (Geometry::POINT, _2D),
        math::Point<double> (x, y)
      { }

    Point (double x,
           double y,
           double z)
      : Geometry(Geometry::POINT, _3D),
        math::Point<double> (x, y, z)
      { }

    ~Point ()
        throw ()
      { }

    //
    // The compiler-generated copy constructor and assignment operator are
    // acceptable.
    //

    //
    // Sets the supplied coordinates of the Point.
    // If getDimension() == _3D, sets z value to 0.
    //
    void
    set (double x,
         double y)
        throw ();

    //
    // Sets the supplied coordinates of the Point.
    // Throws std::out_of_range if getDimension() == _2D.
    //
    void
    set (double x,
         double y,
         double z)
        throw (std::out_of_range);


    //==================================
    //  Geometry INTERFACE
    //==================================


    Geometry*
    clone ()
        const
      { return new Point (*this); }

    std::size_t
        computeWKB_Size()
      const;

    Envelope
    getEnvelope ()
        const
      { return Envelope (x, y, z, x, y, z); }

    void
    toBlob (std::ostream&,
            BlobFormat)                 // Defaults to GEOMETRY.
        const;

    void
    toWKB (std::ostream&,
           bool includeHeader)          // Defaults to true.
        const;

                                        //====================================//
  protected:                            //                      PROTECTED     //
                                        //====================================//

                                        //====================================//
  private:                              //                      PRIVATE       //
                                        //====================================//


    //==================================
    //  Geometry INTERFACE
    //==================================


    void
    changeDimension (Dimension dim)
      {
        if (dim == _2D)
          {
            z = 0;
          }
      }
  };


}                                       // Close feature namespace.
}                                       // Close atakmap namespace.


////========================================================================////
////                                                                        ////
////    EXTERN DECLARATIONS                                                 ////
////                                                                        ////
////========================================================================////

////========================================================================////
////                                                                        ////
////    PUBLIC INLINE DEFINITIONS                                           ////
////                                                                        ////
////========================================================================////


////========================================================================////
////                                                                        ////
////    PROTECTED INLINE DEFINITIONS                                        ////
////                                                                        ////
////========================================================================////

#endif  // #ifndef ATAKMAP_FEATURE_POINT_H_INCLUDED
