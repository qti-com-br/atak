////============================================================================
////
////    FILE:           SpatiaLiteDB.h
////
////    DESCRIPTION:    Implementation of Database interface atop SpatiaLite.
////

////
////
////    HISTORY:
////
////      DATE          AUTHOR          COMMENTS
////      ------------  --------        --------
////      Feb 13, 2015
////
////========================================================================////
////                                                                        ////
////    (c) Copyright 2015 PAR Government Systems Corporation.              ////
////                                                                        ////
////========================================================================////


#ifndef ATAKMAP_DB_SPATIALITE_DB_H_INCLUDED
#define ATAKMAP_DB_SPATIALITE_DB_H_INCLUDED


////========================================================================////
////                                                                        ////
////    INCLUDES AND MACROS                                                 ////
////                                                                        ////
////========================================================================////


#ifndef DB_ASYNC_INTERRUPT
//
// If set to 1, the implementation of SpatiaLiteDB::interrupt will call
// sqlite3_interrupt in a separate thread.  If set to 0, the call is made in the
// calling thread.  In either case, database interruption is asynchronous;
// sqlite3_interrupt merely sets a flag in the database connection.
//
#define DB_ASYNC_INTERRUPT 0
#endif

#include "db/Database.h"

#include "util/NonCopyable.h"
#if DB_ASYNC_INTERRUPT
#include "thread/Cond.h"
#endif
#include "thread/Mutex.h"
#include "util/NonCopyable.h"


////========================================================================////
////                                                                        ////
////    FORWARD DECLARATIONS                                                ////
////                                                                        ////
////========================================================================////


struct sqlite3;


////========================================================================////
////                                                                        ////
////    TYPE DEFINITIONS                                                    ////
////                                                                        ////
////========================================================================////


namespace atakmap                       // Open atakmap namespace.
{
namespace db                            // Open db namespace.
{


///=============================================================================
///
///  class atakmap::db::SpatiaLiteDB
///
///     Implementation of Database interface atop SpatiaLite.
///
///=============================================================================


class SpatiaLiteDB
  : public Database,
    TAK::Engine::Util::NonCopyable
  {
                                        //====================================//
  public:                               //                      PUBLIC        //
                                        //====================================//


    explicit
    SpatiaLiteDB (const char* filePath = NULL)  // Defaults to temporary DB.
        throw (DB_Error);

    SpatiaLiteDB(const char* filePath,
                 bool readOnly)
        throw (DB_Error);

    ~SpatiaLiteDB ()
        throw ()
      { closeConnection (); }

    //
    // The compiler is unable to generate a copy constructor or assignment
    // operator (due to a NonCopyable base class).  This is acceptable.
    //

    //
    // Returns the (possibly NULL) path to the database file.  Returns NULL for
    // a temporary database.
    //
    const char*
    getFilePath ()
        const throw(DB_Error);


    //==================================
    //  db::Database INTERFACE
    //==================================


    void
    beginTransaction ()                 // Establishes an exclusive lock.
        throw (DB_Error);

    db::Statement*
    compileStatement (const char*)
        throw (DB_Error);

    void
    endTransaction ()
        throw (DB_Error);

    void
    execute (const char* sql)
        throw (DB_Error);

    void
    execute (const char* sql,
             const std::vector<const char*>& args)
        throw (DB_Error);

    unsigned long
    getVersion ()
        throw (DB_Error);

    void
    interrupt ()
        throw (DB_Error);

    bool
    inTransaction ()
        const
        throw ()
      { return inTrans; }

    bool
    isReadOnly ()
        const
        throw ()
      { return false; }

    db::Cursor*
    query (const char* sql)
        throw (DB_Error);

    db::Cursor*
    query (const char* sql,
           const std::vector<const char*>& args)
        throw (DB_Error);

    void
    setTransactionSuccessful ()
        throw (DB_Error);

    void
    setVersion (unsigned long)
        throw (DB_Error);


                                        //====================================//
  protected:                            //                      PROTECTED     //
                                        //====================================//


                                        //====================================//
  private:                              //                      PRIVATE       //
                                        //====================================//


    void
    closeConnection ()
        throw ();

    void
    init (const char *filePath)
        throw (DB_Error);

#if DB_ASYNC_INTERRUPT
    static
    void*
    interruptThreadFn (void*);
#endif


    //==================================
    //  PRIVATE REPRESENTATION
    //==================================


    mutable TAK::Engine::Thread::Mutex mutex;
    struct sqlite3* connection;
    void* cache;
    bool inTrans;
    bool successfulTrans;
    bool readOnly;
#if DB_ASYNC_INTERRUPT
    TAK::Engine::Thread::CondVar interruptCV;
    bool interrupting;
    bool finished;
#endif
  };


}                                       // Close db namespace.
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


#endif  // #ifndef ATAKMAP_DB_SPATIALITE_DB_H_INCLUDED
